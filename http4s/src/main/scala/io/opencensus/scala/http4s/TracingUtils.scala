package io.opencensus.scala.http4s

import cats.effect.Effect
import io.opencensus.scala.Tracing
import io.opencensus.scala.http.{
  StatusTranslator,
  HttpAttributes => BaseHttpAttributes
}
import io.opencensus.scala.http4s.HttpAttributes._
import io.opencensus.trace.Span
import org.http4s.Response

object TracingUtils {
  def recordResponse[F[_]: Effect](span: Span, tracing: Tracing)(
      response: Response[F]
  ): Response[F] = {
    BaseHttpAttributes.setAttributesForResponse(span, response)
    tracing.setStatus(span, StatusTranslator.translate(response.status.code))
    response.copy(
      body = response.body.onFinalize(Effect[F].delay(tracing.endSpan(span)))
    )
  }
}
