package com.github.sebruck.opencensus.elastic4s

import com.sksamuel.elastic4s.http.HttpClient
import io.opencensus.trace.Span

import scala.concurrent.ExecutionContext

object implicits {

  implicit class ClientWithTracing(httpClient: HttpClient) {

    /**
      * Enriches the `HttpClient` with tracing `.execute` calls.
      *
      * @param parentSpan the current span which will act as parent of the new span
      */
    def traced(parentSpan: Span)(implicit ec: ExecutionContext): HttpClient =
      TracingHttpClient(httpClient, Some(parentSpan))

    /**
      * Enriches the `HttpClient` with tracing `.execute` calls.
      */
    def traced(implicit ec: ExecutionContext): HttpClient =
      TracingHttpClient(httpClient, None)

  }
}
