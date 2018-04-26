package com.github.sebruck.opencensus.http.propagation

import io.opencensus.trace.propagation.TextFormat
import io.opencensus.trace.{Span, SpanContext, Tracing}

import scala.collection.immutable
import scala.util.Try

trait Propagation[Header, Request] {
  protected val b3Format: TextFormat =
    Tracing.getPropagationComponent.getB3Format

  /**
    * Builds the http headers for the B3 format to propagate the span context
    * to a downstream service.
    *
    * @param span the current span
    * @return a list of B3 headers
    */
  def headersWithTracingContext(span: Span): immutable.Seq[Header]

  /**
    * Extract the spanContext from a http request
    * @param request the (usually incoming) http request
    * @return the span context which can be used as parent context for a new span
    *         if the headers are valid. Otherwise returns a failure containing the error
    */
  def extractContext(request: Request): Try[SpanContext]
}
