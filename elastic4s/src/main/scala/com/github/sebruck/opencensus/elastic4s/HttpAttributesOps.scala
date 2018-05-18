package com.github.sebruck.opencensus.elastic4s

import com.github.sebruck.opencensus.http.{RequestExtractor, ResponseExtractor}
import com.sksamuel.elastic4s.http.HttpResponse

private[elastic4s] object HttpAttributesOps {

  implicit val requestExtractor: RequestExtractor[HttpRequest] =
    new RequestExtractor[HttpRequest] {
      //todo host extraction ???
      override def host(req: HttpRequest): String = "/elasticsearch"

      override def method(req: HttpRequest): String = req.method

      override def path(req: HttpRequest): String = req.endpoint

      override def userAgent(req: HttpRequest): Option[String] = None
    }

  implicit val responseExtractor: ResponseExtractor[HttpResponse] =
    (res: HttpResponse) => res.statusCode.toInt
}
