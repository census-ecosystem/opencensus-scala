package io.opencensus.scala.elastic4s

import com.sksamuel.elastic4s.http.HttpResponse
import io.opencensus.scala.http.HttpAttributesSpec

class HttpAttributesOpsSpec extends HttpAttributesSpec {

  import HttpAttributesOps._

  behavior of "Elastic4s attributes extraction"

  it should "extract method path and host" in {
    val method = "POST"
    val path   = "/index/_search"
    requestExtractor.method(HttpRequest(method, "")) shouldBe method
    requestExtractor.path(HttpRequest(method, path)) shouldBe path
    requestExtractor.host(HttpRequest("", "")) shouldBe "/elasticsearch"
  }

  it should "extract response code" in {
    responseExtractor.statusCode(HttpResponse(999, None, Map.empty)) shouldBe 999
  }
}
