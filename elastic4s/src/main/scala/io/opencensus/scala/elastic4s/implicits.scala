package io.opencensus.scala.elastic4s

import com.sksamuel.elastic4s.ElasticClient
import io.opencensus.trace.Span

object implicits {

  implicit class ClientWithTracing(elastiClient: ElasticClient) {

    /**
      * Enriches the `ElasticClient` with tracing `.execute` calls.
      *
      * @param parentSpan the current span which will act as parent of the new span
      */
    def traced(parentSpan: Span): ElasticClient =
      TracingElasticClient(elastiClient, Some(parentSpan))

    /**
      * Enriches the `ElasticClient` with tracing `.execute` calls.
      */
    def traced: ElasticClient =
      TracingElasticClient(elastiClient, None)

  }
}
