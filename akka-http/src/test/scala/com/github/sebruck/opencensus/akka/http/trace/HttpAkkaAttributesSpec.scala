package com.github.sebruck.opencensus.akka.http.trace

import akka.http.scaladsl.model.headers.{Host, `User-Agent`}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.github.sebruck.opencensus.http.HttpAttributesSpec

class HttpAkkaAttributesSpec extends HttpAttributesSpec {
  import HttpAttributes._

  "Akka http attributes extraction" should behave like httpAttributes(request,
                                                                      response)

  def request: BuildRequest => HttpRequest =
    (request: BuildRequest) =>
      HttpRequest(
        uri = request.host ++ request.path,
        headers = List(`User-Agent`(request.userAgent)) ++ request.hostHeader
          .map(Host(_))
    )

  def response: Int => HttpResponse = HttpResponse(_)
}
