package io.opencensus.scala.akka.http.trace

import akka.http.scaladsl.model.HttpResponse
import io.opencensus.scala.Tracing
import HttpExtractors._
import io.opencensus.scala.akka.http.utils.ExecuteAfterResponse
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
    tracing.setStatus(
      span,
      StatusTranslator.translate(response.status.intValue())
    )

    ExecuteAfterResponse.onComplete(
      response,
      onFinish = () => tracing.endSpan(span),
      onFailure = _ => tracing.endSpan(span)
    )
  }
}
