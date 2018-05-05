package com.github.sebruck.opencensus.http

import com.github.sebruck.opencensus.http.testSuite.MockSpan
import io.opencensus.trace.AttributeValue._
import org.scalatest.{FlatSpec, Matchers}

trait HttpAttributesSpec extends FlatSpec with Matchers {

  def httpAttributes[Request: RequestExtractor, Response: ResponseExtractor](
      request: BuildRequest => Request,
      response: Int => Response): Unit = {

    val span     = new MockSpan("span", None)
    val strValue = stringAttributeValue _

    val (host, path, userAgent) =
      ("example.com", "/this/is/the/path", "agent")

    HttpAttributes.setAttributesForRequest(
      span,
      request(
        BuildRequest("http://" + host + ":8181",
                     path + "?this&not",
                     userAgent,
                     None)))

    behavior of "setAttributesForRequest"

    it should "set the method" in {
      span.attributes("http.method") shouldBe strValue("GET")
    }

    it should "set the path" in {
      span.attributes("http.path") shouldBe strValue(path)
    }

    it should "set the user_agent" in {
      span.attributes("http.user_agent") shouldBe strValue(userAgent)
    }

    it should "set the host from the absolute uri" in {
      span.attributes("http.host") shouldBe strValue(host)
    }

    it should "set the host from the host header if uri is relative" in {
      val span = new MockSpan("span", None)
      HttpAttributes.setAttributesForRequest(
        span,
        request(BuildRequest("", path, userAgent, Some(host))))
      span.attributes("http.host") shouldBe strValue(host)
    }

    behavior of "setAttributesForResponse"

    it should "set the status code" in {
      val span = new MockSpan("span", None)
      HttpAttributes.setAttributesForResponse(span, response(203))
      span
        .attributes("http.status_code") shouldBe longAttributeValue(203L)
    }
  }

  case class BuildRequest(host: String,
                          path: String,
                          userAgent: String,
                          hostHeader: Option[String])
}
