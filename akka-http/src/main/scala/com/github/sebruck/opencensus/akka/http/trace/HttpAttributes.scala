package com.github.sebruck.opencensus.akka.http.trace

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.`User-Agent`
import com.github.sebruck.opencensus.trace.AttributeValueOps._
import io.opencensus.trace.Span

private[http] object HttpAttributes {

  def setAttributesForRequest(span: Span, req: HttpRequest): Unit = {
    req
      .header[`User-Agent`]
      .map(_.value())
      .foreach(span.putAttribute("http.user_agent", _))

    span.putAttribute("http.host", req.uri.authority.host.address())
    span.putAttribute("http.method", req.method.value)
    span.putAttribute("http.path", req.uri.path.toString())
  }

  def setAttributesForResponse(span: Span, resp: HttpResponse): Unit = {
    span.putAttribute("http.status_code", resp.status.intValue().toLong)
  }
}
