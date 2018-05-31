package io.opencensus.scala.akka.http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import io.opencensus.scala.http.testSuite.MockPropagation

object AkkaMockPropagation extends MockPropagation[HttpHeader, HttpRequest] {
  override def rawHeader(key: String, value: String): HttpHeader =
    RawHeader(key, value)
  override def path(request: HttpRequest): String = request.uri.path.toString
}
