package com.github.sebruck.opencensus.http

import com.github.sebruck.opencensus.http.testSuite.MockSpan
import io.opencensus.trace.AttributeValue._
import org.scalatest.{FlatSpec, Matchers}

class HttpAttributesSpec extends FlatSpec with Matchers {

  behavior of "setAttributesForRequest"

  it should "set the attributes" in {
    val span = new MockSpan("span", None)
    val request = MockRequest(
      host = "host",
      method = "method",
      path = "path",
      userAgent = "userAgent"
    )

    val strValue = stringAttributeValue _

    HttpAttributes.setAttributesForRequest(span, request)

    span.attributes("http.host") shouldBe strValue("host")
    span.attributes("http.method") shouldBe strValue("method")
    span.attributes("http.path") shouldBe strValue("path")
    span.attributes("http.user_agent") shouldBe strValue("userAgent")
  }

  behavior of "setAttributesForResponse"

  it should "set the attributes" in {
    val span     = new MockSpan("span", None)
    val response = MockResponse(123)

    HttpAttributes.setAttributesForResponse(span, response)
    span.attributes("http.status_code") shouldBe longAttributeValue(123L)
  }

  case class MockRequest(host: String,
                         method: String,
                         path: String,
                         userAgent: String)

  case class MockResponse(status: Int)

  implicit val requestExtractor: RequestExtractor[MockRequest] =
    new RequestExtractor[MockRequest] {
      override def method(req: MockRequest): String = req.method
      override def userAgent(req: MockRequest): Option[String] =
        Some(req.userAgent)
      override def path(req: MockRequest): String = req.path
      override def host(req: MockRequest): String = req.host
    }

  implicit val responseExtractor: ResponseExtractor[MockResponse] =
    (res: MockResponse) => res.status.toLong
}
