package io.opencensus.scala.http.testSuite

import io.opencensus.scala.http.propagation.Propagation
import io.opencensus.trace._

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

trait MockPropagation[Header, Request] extends Propagation[Header, Request] {

  def rawHeader(key: String, value: String): Header
  def path(request: Request): String

  val requestPathWithoutParent = "/no/parent/context"
  val fakeTraceId              = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  val fakeSpanId               = "bbbbbbbbbbbbbbbb"
  val sampledSpanContext = SpanContext.create(
    TraceId.fromLowerBase16(fakeTraceId),
    SpanId.fromLowerBase16(fakeSpanId),
    TraceOptions.builder().setIsSampled(true).build()
  )

  override def headersWithTracingContext(span: Span): immutable.Seq[Header] =
    List(rawHeader("X-Mock-Trace", "12345"))

  override def extractContext(request: Request): Try[SpanContext] =
    if (path(request) == requestPathWithoutParent)
      Failure(new Exception("test error"))
    else
      Success(sampledSpanContext)

}
