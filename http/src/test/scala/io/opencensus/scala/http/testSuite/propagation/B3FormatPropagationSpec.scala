package io.opencensus.scala.http.testSuite.propagation

import io.opencensus.scala.http.propagation.B3FormatPropagation
import io.opencensus.trace.BlankSpan
import org.scalatest.{FlatSpec, Matchers, TryValues}

import scala.util.Failure

class B3FormatPropagationSpec
    extends FlatSpec
    with Matchers
    with TryValues
    with B3FormatPropagation[(String, String), Map[String, String]] {

  val fakeTraceId = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  val fakeSpanId  = "bbbbbbbbbbbbbbbb"

  "headersWithTracingContext" should "return the correct B3 headers from a spans context" in {
    headersWithTracingContext(BlankSpan.INSTANCE) should contain theSameElementsAs List(
      "X-B3-TraceId" -> "00000000000000000000000000000000",
      "X-B3-SpanId"  -> "0000000000000000"
    )
  }

  behavior of "extractContext"
  it should "return a span context with the values from the B3 http headers" in {
    val request = Map(
      "X-B3-TraceId" -> fakeTraceId,
      "X-B3-SpanId"  -> fakeSpanId,
      "X-B3-Sampled" -> "1"
    )

    val context = extractContext(request).success.value
    context.getTraceId.toLowerBase16 shouldBe fakeTraceId
    context.getSpanId.toLowerBase16 shouldBe fakeSpanId
    context.getTraceOptions.isSampled shouldBe true
  }

  it should "return a failure when the headers are missing" in {
    extractContext(Map.empty) shouldBe a[Failure[_]]
  }

  override def headerValue(
      req: Map[String, String],
      key: String
  ): Option[String] = req.get(key)

  override def createHeader(key: String, value: String): (String, String) =
    (key, value)
}
