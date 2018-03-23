package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import io.opencensus.trace.{Span, SpanContext}

import scala.collection.immutable
import scala.util.Try

trait Propagation {

  /**
    * Builds the http headers for the B3 format to propagate the span context
    * to a downstream service.
    *
    * @param span the current span
    * @return a list of B3 headers
    */
  def headersWithTracingContext(span: Span): immutable.Seq[HttpHeader]

  /**
    * Extract the spanContext from a http request
    * @param request the (usually incoming) http request
    * @return the span context which can be used as parent context for a new span
    *         if the headers are valid. Otherwise returns a failure containing the error
    */
  def extractContext(request: HttpRequest): Try[SpanContext]
}
