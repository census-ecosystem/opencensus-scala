package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.github.sebruck.opencensus.Tracing
import com.github.sebruck.opencensus.akka.http.propagation.B3FormatPropagation
import io.opencensus.trace.{Span, Status}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait TracingClient {
  protected val tracing: Tracing
  protected val propagation: Propagation
  implicit protected val ec: ExecutionContext

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
      import tracing._

      val span            = startSpanWithParent(request.uri.path.toString, parentSpan)
      val enrichedRequest = requestWithTraceContext(request, span)
      val result          = doRequest(enrichedRequest)

      result.onComplete {
        case Success(response) =>
          endSpan(span, StatusTranslator.translate(response.status))
        case Failure(_) =>
          endSpan(span, Status.INTERNAL)
      }

      result
  }

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
