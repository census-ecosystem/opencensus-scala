package com.github.sebruck.opencensus.http4s

import com.github.sebruck.opencensus.http.{RequestExtractor, ResponseExtractor}
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Request, Response}

private[http4s] object HttpAttributes {

  implicit def requestExtractor[F[_]]: RequestExtractor[Request[F]] =
    new RequestExtractor[Request[F]] {
      override def method(req: Request[F]): String = req.method.name

      override def userAgent(req: Request[F]): Option[String] =
        req.headers.get(CaseInsensitiveString("User-Agent")).map(_.value)

      override def path(req: Request[F]): String = req.uri.path.toString

      override def host(req: Request[F]): String = {

        val hostHeader = req.headers
          .get(CaseInsensitiveString("Host"))
          .map(_.value)

        req.uri.authority
          .map(_.host.value)
          .getOrElse(
            hostHeader
            // Having no Host header with a relative URL is invalid according to rfc2616,
            // but http4s still allows to create such HttpRequests.
              .getOrElse(""))
      }
    }

  implicit def responseExtractor[F[_]]: ResponseExtractor[Response[F]] =
    (res: Response[F]) => res.status.code.toLong
}
