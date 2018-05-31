package io.opencensus.scala.http4s

import cats.effect.Effect
import io.opencensus.trace.Span
import org.http4s.client.Client

object implicits {

  implicit class ClientWithTracing[F[_]: Effect](client: Client[F]) {

    /**
      * Enriches the `Client[F]` by tracing and propagation of the SpanContext via http headers.
      *
      * @param parentSpan the current span which will act as parent of the new span
      */
    def traced(parentSpan: Span): Client[F] =
      TracingClient[F].trace(client, Some(parentSpan))

    /**
      * Enriches the `Client[F]` by tracing and propagation of the SpanContext via http headers.
      */
    def traced: Client[F] =
      TracingClient[F].trace(client, None)
  }
}
