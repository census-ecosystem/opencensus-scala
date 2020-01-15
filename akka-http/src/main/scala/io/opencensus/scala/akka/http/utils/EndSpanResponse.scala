package io.opencensus.scala.akka.http.utils

import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import io.opencensus.scala.Tracing
import io.opencensus.scala.akka.http.trace.HttpAttributes._
import io.opencensus.scala.http.{HttpAttributes, StatusTranslator}
import io.opencensus.trace.Span

private[http] object EndSpanResponse {

  def forServer(
      tracing: Tracing,
      response: HttpResponse,
      span: Span
  ): HttpResponse =
    end(tracing, response, span, "response sent")

  def forClient(
      tracing: Tracing,
      response: HttpResponse,
      span: Span
  ): HttpResponse =
    end(tracing, response, span, "response received")

  private def end(
      tracing: Tracing,
      response: HttpResponse,
      span: Span,
      responseAnnotation: String
  ): HttpResponse = {

    HttpAttributes.setAttributesForResponse(span, response)
    span.addAnnotation(responseAnnotation)

    val status = StatusTranslator.translate(response.status.intValue())
    // todo use new setStatus method here when merged
    response.copy(
      entity = if (response.status.allowsEntity) {
        response.entity.transformDataBytes(
          EndSpanFlow(
            span,
            tracing,
            status
          )
        )
      } else {
        tracing.endSpan(span, status)
        HttpEntity.Empty
      }
    )
  }
}
