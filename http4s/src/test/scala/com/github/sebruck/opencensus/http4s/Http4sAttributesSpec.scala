package com.github.sebruck.opencensus.http4s

import cats.Id
import com.github.sebruck.opencensus.http.HttpAttributesSpec
import org.http4s.{Header, Headers, Request, Response, Status, Uri}

class Http4sAttributesSpec extends HttpAttributesSpec {
  import HttpAttributes._

  "Http4s attributes extraction" should behave like httpAttributes(request,
                                                                   response)

  def request: BuildRequest => Request[Id] =
    (request: BuildRequest) =>
      Request[Id](
        uri = Uri.unsafeFromString(request.host ++ request.path),
        headers = Headers(
          List(Header("User-Agent", request.userAgent)) ++ request.hostHeader
            .map(Header("Host", _)))
    )

  def response: Int => Response[Id] =
    (code: Int) => Response[Id](Status(code))
}
