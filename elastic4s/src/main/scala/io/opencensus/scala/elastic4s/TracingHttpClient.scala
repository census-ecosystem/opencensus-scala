package io.opencensus.scala.elastic4s

import com.sksamuel.elastic4s.http._
import io.opencensus.scala.Tracing
import io.opencensus.scala.elastic4s.HttpAttributesOps._
import io.opencensus.scala.http.{HttpAttributes, StatusTranslator}
import io.opencensus.trace.{Span, Status}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class HttpRequest(method: String, endpoint: String)

/**
  * Enriches the `HttpClient` with tracing `.execute` calls.
  *
  * @param c the `HttpClient` to be enriched.
  * @param T the `Tracing` that is used to start and end `Span`.
  * @param parentSpan the current span which will act as parent of the new span
  */
class TracingHttpClient(c: HttpClient, T: Tracing, parentSpan: Option[Span])(
    implicit ec: ExecutionContext
) extends HttpClient {
  import T._

  override def client: HttpRequestClient = new HttpRequestClient {

    override def async(
        method: String,
        endpoint: String,
        params: Map[String, Any]
    ): Future[HttpResponse] = {
      val span = startSpanFor(method, endpoint)

      c.client
        .async(method, endpoint, params)
        .transform(end(span))
    }

    override def async(
        method: String,
        endpoint: String,
        params: Map[String, Any],
        entity: HttpEntity
    ): Future[HttpResponse] = {
      val span = startSpanFor(method, endpoint)

      c.client
        .async(method, endpoint, params, entity)
        .transform(end(span))

    }

    override def close(): Unit = c.client.close()
  }

  private def startSpanFor(method: String, endpoint: String): Span = {
    val span =
      parentSpan.fold(startSpan(endpoint))(startSpanWithParent(endpoint, _))

    HttpAttributes.setAttributesForRequest(span, HttpRequest(method, endpoint))

    span
  }

  private def endSpanSuccess(response: HttpResponse, span: Span): Unit = {
    HttpAttributes.setAttributesForResponse(span, response)
    endSpan(span, StatusTranslator.translate(response.statusCode))
  }

  private def endSpanError(span: Span): Unit = endSpan(span, Status.INTERNAL)

  private def end(span: Span): Try[HttpResponse] => Try[HttpResponse] = {
    case s @ Success(res) =>
      endSpanSuccess(res, span)
      s
    case f @ Failure(_) =>
      endSpanError(span)
      f
  }

  override def close(): Unit = client.close()
}
object TracingHttpClient {

  /**
    * Enriches the `HttpClient` with tracing `.execute` calls.
    * Uses [[io.opencensus.scala.Tracing]] as Tracing
    *
    * @param c the `HttpClient` to be enriched.
    * @param parentSpan the current span which will act as parent of the new span
    */
  def apply(c: HttpClient, parentSpan: Option[Span])(
      implicit ec: ExecutionContext
  ): TracingHttpClient =
    new TracingHttpClient(c, Tracing, parentSpan)
}
