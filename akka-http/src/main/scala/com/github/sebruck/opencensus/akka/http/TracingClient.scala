package com.github.sebruck.opencensus.akka.http

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, GraphDSL, UnzipWith, Zip}
import akka.stream.{FlowShape, OverflowStrategy}
import com.github.sebruck.opencensus.Tracing
import com.github.sebruck.opencensus.akka.http.propagation.B3FormatPropagation
import com.github.sebruck.opencensus.akka.http.trace.HttpAttributes
import io.opencensus.trace.{Span, Status}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait TracingClient {
  protected val tracing: Tracing
  protected val propagation: Propagation
  implicit protected val ec: ExecutionContext

  import tracing._

  /**
    * Enriches the doRequest function by tracing and propagation of the SpanContext via http headers.
    *
    * @param doRequest the function which executes the HttpRequest, usually Http.singleRequest
    * @param parentSpan the current span which will act as parent of the new span
    * @return the enriched function
    */
  def traceRequest(doRequest: HttpRequest => Future[HttpResponse],
                   parentSpan: Span): HttpRequest => Future[HttpResponse] = {
    request =>
      val (enrichedRequest, span) =
        startSpanAndEnrichRequest(request, parentSpan)
      val result = doRequest(enrichedRequest)

      result.onComplete {
        case Success(response) => endSpanSuccess(response, span)
        case Failure(_)        => endSpanError(span)
      }

      result
  }

  /**
    * Enriches a `Flow[HttpRequest, HttpResponse, _]` by tracing and propagation of the SpanContext via
    * http headers.
    *
    * @param connection the flow, usually this is the return value of Http().outgoingConnection()
    * @param parentSpan the current span which will act as parent of the new span
    * @return the enriched flow
    */
  def traceRequest(connection: Flow[HttpRequest, HttpResponse, _],
                   parentSpan: Span)
    : Flow[HttpRequest, HttpResponse, NotUsed] = { // Todo: How can i preserve the mat value?

    val startSpan = Flow[HttpRequest]
      .map(startSpanAndEnrichRequest(_, parentSpan))

    val errorToTry = Flow[HttpResponse]
      .map(Success(_))
      .recover { case NonFatal(e) => Failure(e) }

    val doRequest = Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val bcast =
        b.add(UnzipWith[(HttpRequest, Span), HttpRequest, Span](identity))
      val zip = b.add(Zip[Try[HttpResponse], Span])

      // This buffer is needed because otherwise the zip will backpressure. This backpressure prevents grouping,
      // which leads to a state where every downstream consumer can just group by a maximum of one.
      val bufferForZip = Flow[Span].buffer(1000, OverflowStrategy.backpressure)

      // format: off
      bcast.out0 ~> connection   ~> errorToTry ~> zip.in0
      bcast.out1 ~> bufferForZip               ~> zip.in1
      // format: on

      FlowShape(bcast.in, zip.out)
    })

    val endSpan = Flow[(Try[HttpResponse], Span)]
      .map({
        case (Success(response), span) =>
          endSpanSuccess(response, span)
          response
        case (Failure(e), span) =>
          endSpanError(span)
          throw e
      })

    startSpan
      .via(doRequest)
      .via(endSpan)
  }

  private def startSpanAndEnrichRequest(
      request: HttpRequest,
      parentSpan: Span): (HttpRequest, Span) = {
    val span = startSpanWithParent(request.uri.path.toString, parentSpan)
    HttpAttributes.setAttributesForRequest(span, request)
    val enrichedRequest = requestWithTraceContext(request, span)

    (enrichedRequest, span)
  }

  private def endSpanSuccess(response: HttpResponse, span: Span): Unit = {
    HttpAttributes.setAttributesForResponse(span, response)
    endSpan(span, StatusTranslator.translate(response.status))
  }

  private def endSpanError(span: Span): Unit = endSpan(span, Status.INTERNAL)

  private def requestWithTraceContext(request: HttpRequest,
                                      span: Span): HttpRequest = {
    val traceHeaders = propagation.headersWithTracingContext(span)
    request.mapHeaders(_ ++ traceHeaders)
  }
}

object TracingClient extends TracingClient {
  import scala.concurrent.ExecutionContext.Implicits.global
  override protected val tracing: Tracing              = Tracing
  override protected val propagation: Propagation      = B3FormatPropagation
  override implicit protected val ec: ExecutionContext = global
}
