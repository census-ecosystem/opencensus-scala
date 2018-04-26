package com.github.sebruck.opencensus.http4s

import com.github.sebruck.opencensus.trace.AttributeValueOps._
import io.opencensus.trace.Span
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Request, Response}

private[http4s] object HttpAttributes {

  def setAttributesForRequest[F[_]](span: Span, req: Request[F]): Unit = {
    req.headers
      .get(CaseInsensitiveString("User-Agent"))
      .map(_.value)
      .foreach(span.putAttribute("http.user_agent", _))

    req.uri.authority.foreach(auth =>
      span.putAttribute("http.host", auth.host.value))

    span.putAttribute("http.method", req.method.name)
    span.putAttribute("http.path", req.uri.path.toString())
  }

  def setAttributesForResponse[F[_]](span: Span, resp: Response[F]): Unit = {
    span.putAttribute("http.status_code", resp.status.code.toLong)
  }
}
