package io.opencensus.scala.http4s.propagation

import io.opencensus.scala.http.propagation.B3FormatPropagation
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Request}

private[http4s] trait Http4sFormatPropagation[F[_]]
    extends B3FormatPropagation[Header, Request[F]] {
  override def headerValue(req: Request[F], key: String): Option[String] =
    req.headers
      .get(CaseInsensitiveString(key.toLowerCase))
      .map(_.value)

  override def createHeader(key: String, value: String): Header =
    Header(key, value)
}
