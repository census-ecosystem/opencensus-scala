package com.github.sebruck.opencensus.akka.http.trace

import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.model.headers.{`User-Agent`, Host}
import org.scalatest.{FlatSpec, Matchers}

class HttpAttributesSpec extends FlatSpec with Matchers {
  import HttpAttributes._

  behavior of "requestExtractor"

  it should "extract the method" in {
    val req = HttpRequest(HttpMethods.DELETE)
    requestExtractor.method(req) shouldBe "DELETE"
  }

  it should "extract the path" in {
    val req = HttpRequest(uri = "http://example.com/this/is/the/path?this&not")
    requestExtractor.path(req) shouldBe "/this/is/the/path"
  }

  it should "extract the user_agent" in {
    val req = HttpRequest(headers = List(`User-Agent`("my-agent")))
    requestExtractor.userAgent(req) shouldBe Some("my-agent")
  }

  it should "extract the host from the absolute uri" in {
    val req = HttpRequest(uri = "http://example.com:8181/abc")
    requestExtractor.host(req) shouldBe "example.com"
  }

  it should "extract the host from the host header if uri is relative" in {
    val req = HttpRequest(uri = "/abc", headers = List(Host("example.com")))
    requestExtractor.host(req) shouldBe "example.com"
  }
}
