package com.github.sebruck.opencensus.akka.http.trace

import akka.http.scaladsl.model.headers.{`User-Agent`, Host}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.github.sebruck.opencensus.http.{RequestExtractor, ResponseExtractor}

object HttpAttributes {

  implicit val requestExtractor: RequestExtractor[HttpRequest] =
    new RequestExtractor[HttpRequest] {
      override def host(req: HttpRequest): String = {
        if (req.uri.isAbsolute)
          req.uri.authority.host.address()
        else
          req
            .header[Host]
            .map(_.value())
            // Having no Host header with a relative URL is invalid according to rfc2616,
            // but Akka-HTTP still allows to create such HttpRequests.
            .getOrElse("")
      }

      override def method(req: HttpRequest): String = req.method.value

      override def path(req: HttpRequest): String = req.uri.path.toString()

      override def userAgent(req: HttpRequest): Option[String] =
        req
          .header[`User-Agent`]
          .map(_.value())
    }

  implicit val responseExtractor: ResponseExtractor[HttpResponse] =
    (res: HttpResponse) => res.status.intValue().toLong
}
