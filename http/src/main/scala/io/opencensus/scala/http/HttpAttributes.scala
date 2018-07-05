package io.opencensus.scala.http

import io.opencensus.scala.trace.AttributeValueOps._
import io.opencensus.trace.Span

object HttpAttributes {
  def setAttributesForRequest[R](span: Span, req: R)(
      implicit ext: RequestExtractor[R]
  ): Unit = {
    ext.userAgent(req).foreach(span.putAttribute("http.user_agent", _))
    span.putAttribute("http.host", ext.host(req))
    span.putAttribute("http.method", ext.method(req))
    span.putAttribute("http.path", ext.path(req))
  }

  def setAttributesForResponse[R](span: Span, resp: R)(
      implicit ext: ResponseExtractor[R]
  ): Unit = {
    span.putAttribute("http.status_code", ext.statusCode(resp))
  }
}

trait RequestExtractor[R] {
  def host(req: R): String
  def method(req: R): String
  def path(req: R): String
  def userAgent(req: R): Option[String]
}

trait ResponseExtractor[R] {
  def statusCode(res: R): Long
}
