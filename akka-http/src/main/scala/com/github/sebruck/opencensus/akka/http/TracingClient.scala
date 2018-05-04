package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, UnzipWith, Zip}
import akka.stream.{FlowShape, OverflowStrategy}
import com.github.sebruck.opencensus.Tracing
import com.github.sebruck.opencensus.akka.http.propagation.AkkaB3FormatPropagation
import com.github.sebruck.opencensus.http.{HttpAttributes, StatusTranslator}
import com.github.sebruck.opencensus.http.propagation.Propagation
import com.github.sebruck.opencensus.akka.http.trace.HttpAttributes._
import io.opencensus.trace.{Span, Status}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait TracingClient {
  protected val tracing: Tracing
  protected val propagation: Propagation[HttpHeader, HttpRequest]
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
                   parentSpan: Span): HttpRequest => Future[HttpResponse] =
    traceRequest(doRequest, Some(parentSpan))

  /**
    * Enriches the doRequest function by tracing and propagation of the SpanContext via http headers.
    *
    * @param doRequest the function which executes the HttpRequest, usually Http.singleRequest
    * @return the enriched function
    */
  def traceRequest(doRequest: HttpRequest => Future[HttpResponse])
    : HttpRequest => Future[HttpResponse] = traceRequest(doRequest, None)

  /**
    * Enriches a `Flow[HttpRequest, HttpResponse, _]`, which is usually returned by `Http().outgoingConnection`,
    * with tracing and propagation of the SpanContext via http headers.
    *
    * @param connection the flow, usually this is the return value of `Http().outgoingConnection`
    * @param parentSpan the current span which will act as parent of the new span
    * @return the enriched flow
    */
  def traceRequestForConnection[Mat](
      connection: Flow[HttpRequest, HttpResponse, Mat],
      parentSpan: Span): Flow[HttpRequest, HttpResponse, Mat] =
    traceRequestForConnection(connection, Some(parentSpan))

  /**
    * Enriches a `Flow[HttpRequest, HttpResponse, _]`, which is usually returned by `Http().outgoingConnection`,
    * with tracing and propagation of the SpanContext via http headers.
    *
    * @param connection the flow, usually this is the return value of `Http().outgoingConnection`
    * @return the enriched flow
    */
  def traceRequestForConnection[Mat](
      connection: Flow[HttpRequest, HttpResponse, Mat])
    : Flow[HttpRequest, HttpResponse, Mat] =
    traceRequestForConnection(connection, None)

  /**
    * Enriches a `Flow[(HttpRequest, T), (Try[HttpResponse], T), _]`, which is usually returned by
    * `Http().cachedHostConnectionPool` with tracing and propagation of the SpanContext via http headers.
    *
    * @param connectionPool the flow, usually this is the return value of  `Http().cachedHostConnectionPool`
    * @param parentSpan the current span which will act as parent of the new span
    * @return the enriched flow
    */
  def traceRequestForPool[T, Mat](
      connectionPool: Flow[(HttpRequest, T), (Try[HttpResponse], T), Mat],
      parentSpan: Span): Flow[(HttpRequest, T), (Try[HttpResponse], T), Mat] =
    traceRequestForPool(connectionPool, Some(parentSpan))

  /**
    * Enriches a `Flow[(HttpRequest, T), (Try[HttpResponse], T), _]`, which is usually returned by
    * `Http().cachedHostConnectionPool` with tracing and propagation of the SpanContext via http headers.
    *
    * @param connectionPool the flow, usually this is the return value of  `Http().cachedHostConnectionPool`
    * @return the enriched flow
    */
  def traceRequestForPool[T, Mat](
      connectionPool: Flow[(HttpRequest, T), (Try[HttpResponse], T), Mat]
  ): Flow[(HttpRequest, T), (Try[HttpResponse], T), Mat] =
    traceRequestForPool(connectionPool, None)

  private def traceRequest(
      doRequest: HttpRequest => Future[HttpResponse],
      parentSpan: Option[Span]): HttpRequest => Future[HttpResponse] =
    request => {
      val (enrichedRequest, span) =
        startSpanAndEnrichRequest(request, parentSpan)
      val result = doRequest(enrichedRequest)

      result.onComplete {
        case Success(response) => endSpanSuccess(response, span)
        case Failure(_)        => endSpanError(span)
      }

      result
    }

  private def traceRequestForConnection[Mat](
      connection: Flow[HttpRequest, HttpResponse, Mat],
      parentSpan: Option[Span]): Flow[HttpRequest, HttpResponse, Mat] = {

    val startSpan = Flow[HttpRequest]
      .map(startSpanAndEnrichRequest(_, parentSpan))

    val errorToTry = Flow[HttpResponse]
      .map(Success(_))
      .recover { case NonFatal(e) => Failure(e) }

    val doRequest = spanForwardingFlow(connection.via(errorToTry))

    val endSpan = Flow[(Try[HttpResponse], Span)]
      .map {
        case (Success(response), span) =>
          endSpanSuccess(response, span)
          response
        case (Failure(e), span) =>
          endSpanError(span)
          throw e
      }

    startSpan
      .viaMat(doRequest)(Keep.right)
      .viaMat(endSpan)(Keep.left)
  }

  def traceRequestForPool[T, Mat](
      connectionPool: Flow[(HttpRequest, T), (Try[HttpResponse], T), Mat],
      parentSpan: Option[Span])
    : Flow[(HttpRequest, T), (Try[HttpResponse], T), Mat] = {

    val startSpan = Flow[(HttpRequest, T)]
      .map {
        case (request, context) =>
          val (enrichedRequest, span) =
            startSpanAndEnrichRequest(request, parentSpan)
          ((enrichedRequest, context), span)
      }

    val doRequest = spanForwardingFlow(connectionPool)

    val endSpan = Flow[((Try[HttpResponse], T), Span)]
      .map {
        case ((Success(response), context), span) =>
          endSpanSuccess(response, span)
          (Success(response), context)
        case ((Failure(error), context), span) =>
          endSpanError(span)
          (Failure(error), context)
      }

    startSpan
      .viaMat(doRequest)(Keep.right)
      .viaMat(endSpan)(Keep.left)
  }

  private def spanForwardingFlow[In, Out, Mat](underlying: Flow[In, Out, Mat]) =
    Flow.fromGraph(GraphDSL.create(underlying) { implicit b => under =>
      import GraphDSL.Implicits._

      val bcast =
        b.add(UnzipWith[(In, Span), In, Span](identity))
      val zip = b.add(Zip[Out, Span])

      // This buffer is needed because otherwise the zip will backpressure. This backpressure prevents grouping,
      // which leads to a state where every downstream consumer can just group by a maximum of one.
      val bufferForZip = Flow[Span].buffer(1000, OverflowStrategy.backpressure)

      // format: off
      bcast.out0 ~> under        ~> zip.in0
      bcast.out1 ~> bufferForZip ~> zip.in1
      // format: on

      FlowShape(bcast.in, zip.out)
    })

  private def startSpanAndEnrichRequest(
      request: HttpRequest,
      parentSpan: Option[Span]): (HttpRequest, Span) = {
    val name = request.uri.path.toString
    val span = parentSpan.fold(startSpan(name))(startSpanWithParent(name, _))

    HttpAttributes.setAttributesForRequest(span, request)
    val enrichedRequest = requestWithTraceContext(request, span)

    (enrichedRequest, span)
  }

  private def endSpanSuccess(response: HttpResponse, span: Span): Unit = {
    HttpAttributes.setAttributesForResponse(span, response)
    endSpan(span, StatusTranslator.translate(response.status.intValue()))
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
  override protected val tracing: Tracing = Tracing
  override protected val propagation: Propagation[HttpHeader, HttpRequest] =
    AkkaB3FormatPropagation
  override implicit protected val ec: ExecutionContext = global
}
