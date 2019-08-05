package io.opencensus.scala.elastic4s

import com.sksamuel.elastic4s.{
  ElasticClient,
  ElasticRequest,
  HttpClient,
  HttpResponse
}
import io.opencensus.scala.Tracing
import io.opencensus.scala.elastic4s.HttpAttributesOps._
import io.opencensus.scala.http.{HttpAttributes, StatusTranslator}
import io.opencensus.trace.{Span, Status}

case class HttpRequest(method: String, endpoint: String)

object TracingElasticClient {

  /**
    * Enriches the `ElasticClient` with tracing `.execute` calls.
    * Uses io.opencensus.scala.Tracing as Tracing
    *
    * @param c the `ElasticClient` to be enriched.
    * @param parentSpan the current span which will act as parent of the new span
    */
  def apply(c: ElasticClient, parentSpan: Option[Span]): ElasticClient =
    tracingElasticClient(c, Tracing, parentSpan)

  def tracingElasticClient(
      c: ElasticClient,
      T: Tracing,
      parentSpan: Option[Span]
  ): ElasticClient = {
    import T._

    def startSpanFor(method: String, endpoint: String): Span = {
      val span =
        parentSpan.fold(startSpan(endpoint))(startSpanWithParent(endpoint, _))

      HttpAttributes.setAttributesForRequest(
        span,
        HttpRequest(method, endpoint)
      )

      span
    }

    def endSpanSuccess(response: HttpResponse, span: Span): Unit = {
      HttpAttributes.setAttributesForResponse(span, response)
      endSpan(span, StatusTranslator.translate(response.statusCode))
    }

    def endSpanError(span: Span): Unit = endSpan(span, Status.INTERNAL)

    def end(
        span: Span
    ): Either[Throwable, HttpResponse] => Either[Throwable, HttpResponse] = {
      case r @ Right(res) =>
        endSpanSuccess(res, span)
        r
      case l @ Left(_) =>
        endSpanError(span)
        l
    }

    c.copy(new HttpClient {
      override def send(
          request: ElasticRequest,
          callback: Either[Throwable, HttpResponse] => Unit
      ): Unit = {
        val span = startSpanFor(request.method, request.endpoint)
        c.client.send(request, callback.compose(end(span)))
      }
      override def close(): Unit = c.client.close()
    })
  }
}
