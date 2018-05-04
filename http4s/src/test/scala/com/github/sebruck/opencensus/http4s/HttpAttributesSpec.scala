package com.github.sebruck.opencensus.http4s

import org.http4s.{Header, Headers, Method, Request, Uri}
import org.scalatest.{FlatSpec, Matchers}

class HttpAttributesSpec extends FlatSpec with Matchers {
  import HttpAttributes._

  behavior of "requestExtractor"

  it should "extract the method" in {
    val req = Request(Method.DELETE)
    requestExtractor[Nothing].method(req) shouldBe "DELETE"
  }

  it should "extract the path" in {
    val req =
      Request(
        uri =
          Uri.unsafeFromString("http://example.com/this/is/the/path?this&not"))
    requestExtractor[Nothing].path(req) shouldBe "/this/is/the/path"
  }

  it should "extract the user_agent" in {
    val req = Request(headers = Headers(Header("User-Agent", "my-agent")))
    requestExtractor[Nothing].userAgent(req) shouldBe Some("my-agent")
  }

  it should "extract the host from the absolute uri" in {
    val req = Request(uri = Uri.unsafeFromString("http://example.com:8181/abc"))
    requestExtractor[Nothing].host(req) shouldBe "example.com"
  }

  it should "extract the host from the host header if uri is relative" in {
    val req = Request(uri = Uri.unsafeFromString("/abc"),
                      headers = Headers(Header("Host", "example.com")))
    requestExtractor[Nothing].host(req) shouldBe "example.com"
  }
}
