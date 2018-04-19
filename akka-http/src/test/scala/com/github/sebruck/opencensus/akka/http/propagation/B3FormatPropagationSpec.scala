package com.github.sebruck.opencensus.akka.http.propagation

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import com.github.sebruck.opencensus.akka.http.AkkaMockPropagation._
import io.opencensus.trace.BlankSpan
import org.scalatest.{FlatSpec, Matchers, TryValues}

import scala.util.Failure

class B3FormatPropagationSpec
    extends FlatSpec
    with Matchers
    with TryValues
    with B3FormatPropagation {

  "headersWithTracingContext" should "return the correct B3 headers from a spans context" in {
    val headers = headersWithTracingContext(BlankSpan.INSTANCE)
      .map(header => (header.name(), header.value()))

    headers should contain theSameElementsAs List(
      "X-B3-TraceId" -> "00000000000000000000000000000000",
      "X-B3-SpanId"  -> "0000000000000000")
  }

  behavior of "extractContext"
  it should "return a span context with the values from the B3 http headers" in {
    val request = HttpRequest(
      headers = List(
        RawHeader("X-B3-TraceId", fakeTraceId),
        RawHeader("X-B3-SpanId", fakeSpanId),
        RawHeader("X-B3-Sampled", "1")
      ))

    val context = extractContext(request).success.value
    context.getTraceId.toLowerBase16 shouldBe fakeTraceId
    context.getSpanId.toLowerBase16 shouldBe fakeSpanId
    context.getTraceOptions.isSampled shouldBe true
  }

  it should "return a failure when the headers are missing" in {
    extractContext(HttpRequest()) shouldBe a[Failure[_]]
  }
}
