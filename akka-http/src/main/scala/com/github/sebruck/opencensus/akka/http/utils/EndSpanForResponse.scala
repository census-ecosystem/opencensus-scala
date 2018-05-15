package com.github.sebruck.opencensus.akka.http.utils

import akka.http.scaladsl.model.HttpResponse
import com.github.sebruck.opencensus.Tracing
import com.github.sebruck.opencensus.http.{HttpAttributes, StatusTranslator}
import io.opencensus.trace.Span
import com.github.sebruck.opencensus.akka.http.trace.HttpAttributes._

private[http] object EndSpanForResponse {

  def apply(tracing: Tracing,
            response: HttpResponse,
            span: Span): HttpResponse = {

    HttpAttributes.setAttributesForResponse(span, response)
    span.addAnnotation("Http Response Received") // TODO check census spec

    // todo use new setStatus method here when merged
    response.copy(
      entity = response.entity.transformDataBytes(
        EndSpanFlow(span,
                    tracing,
                    StatusTranslator.translate(response.status.intValue()))))
  }
}
