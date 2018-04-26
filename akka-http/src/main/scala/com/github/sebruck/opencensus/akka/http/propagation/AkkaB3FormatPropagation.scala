package com.github.sebruck.opencensus.akka.http.propagation

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import com.github.sebruck.opencensus.http.propagation.B3FormatPropagation

private[http] object AkkaB3FormatPropagation
    extends B3FormatPropagation[HttpHeader, HttpRequest] {

  override def headerValue(req: HttpRequest, key: String): Option[String] =
    req.headers
      .find(_.lowercaseName() == key.toLowerCase)
      .map(_.value())

  override def createHeader(key: String, value: String): HttpHeader =
    RawHeader(key, value)
}
