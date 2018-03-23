package com.github.sebruck.opencensus.akka.http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import io.opencensus.trace._

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

object MockPropagation extends Propagation {

  val requestPathWithoutParent = "/no/parent/context"
  val fakeTraceId              = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  val fakeSpanId               = "bbbbbbbbbbbbbbbb"
  val sampledSpanContext = SpanContext.create(
    TraceId.fromLowerBase16(fakeTraceId),
    SpanId.fromLowerBase16(fakeSpanId),
    TraceOptions.builder().setIsSampled(true).build()
  )

  override def headersWithTracingContext(
      span: Span): immutable.Seq[HttpHeader] =
    List(RawHeader("X-Mock-Trace", "12345"))

  override def extractContext(request: HttpRequest): Try[SpanContext] =
    if (request.uri.path.toString == requestPathWithoutParent)
      Failure(new Exception("test error"))
    else
      Success(sampledSpanContext)

}
