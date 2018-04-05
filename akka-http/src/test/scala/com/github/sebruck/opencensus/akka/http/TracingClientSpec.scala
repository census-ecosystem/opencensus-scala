package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import com.github.sebruck.opencensus.Tracing
import io.opencensus.trace.{BlankSpan, SpanContext, Status}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers, OptionValues}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class TracingClientSpec
    extends FlatSpec
    with Matchers
    with ScalaFutures
    with OptionValues {

  behavior of "traceRequest"

  it should "enrich the HttpRequest with propagation headers" in {
    val (client, _) = clientWithMock()

    client
      .traceRequest({ request =>
        request.headers should contain(RawHeader("X-Mock-Trace", "12345"))
        Future.successful(HttpResponse())
      }, BlankSpan.INSTANCE)(HttpRequest())
      .futureValue
  }

  it should "keep existing headers" in {
    val (client, _) = clientWithMock()
    val header      = RawHeader("Foo", "Bar")
    val request     = HttpRequest(headers = List(RawHeader("Foo", "Bar")))

    client
      .traceRequest(
        { request =>
          request.headers should contain(RawHeader("X-Mock-Trace", "12345"))
          request.headers should contain(header)
          Future.successful(HttpResponse())
        },
        BlankSpan.INSTANCE
      )(request)
      .futureValue
  }

  it should "start and end a span with parent context when the request succeeds" in {
    val (client, mockTracing) = clientWithMock()

    client
      .traceRequest(_ => Future.successful(HttpResponse()), BlankSpan.INSTANCE)(
        HttpRequest(uri = "/test"))
      .futureValue

    val startedSpan = mockTracing.startedSpans.headOption.value

    startedSpan.name shouldBe "/test"
    startedSpan.parentContext.value shouldBe SpanContext.INVALID
    mockTracing.endedSpansStatuses should contain(Status.OK)
  }

  it should "end a span when the request fails" in {
    val (client, mockTracing) = clientWithMock()

    client
      .traceRequest(_ => Future.failed(new Exception("Test Error")),
                    BlankSpan.INSTANCE)(HttpRequest(uri = "/test"))
      .failed
      .futureValue

    mockTracing.endedSpansStatuses.map(_.getCanonicalCode) should contain(
      Status.INTERNAL.getCanonicalCode)
  }

  it should "return the http response in case of success" in {
    val (client, _) = clientWithMock()

    val result = client
      .traceRequest(_ => Future.successful(HttpResponse(StatusCodes.ImATeapot)),
                    BlankSpan.INSTANCE)(HttpRequest(uri = "/test"))
      .futureValue

    result.status shouldBe StatusCodes.ImATeapot
  }

  it should "return the exception in case of failure" in {
    val (client, _) = clientWithMock()

    val result = client
      .traceRequest(_ => Future.failed(new Exception("Test error")),
                    BlankSpan.INSTANCE)(HttpRequest(uri = "/test"))
      .failed
      .futureValue

    result.getMessage shouldBe "Test error"
  }

  it should "set the http attributes" in {
    import io.opencensus.trace.AttributeValue._

    val (client, mock) = clientWithMock()
    val request        = HttpRequest(uri = "http://example.com/my/fancy/path")

    client
      .traceRequest(_ => Future.successful(HttpResponse()), BlankSpan.INSTANCE)(
        request)
      .futureValue

    val attributes = mock.startedSpans.headOption.value.attributes

    attributes.get("http.host").value shouldBe stringAttributeValue(
      "example.com")
    attributes.get("http.path").value shouldBe stringAttributeValue(
      "/my/fancy/path")
    attributes.get("http.method").value shouldBe stringAttributeValue("GET")
    attributes.get("http.status_code").value shouldBe longAttributeValue(200L)
  }

  def clientWithMock() = {
    val mockTracing = new MockTracing
    val client = new TracingClient {
      override protected val tracing: Tracing              = mockTracing
      override protected val propagation: Propagation      = MockPropagation
      override implicit protected val ec: ExecutionContext = global
    }

    (client, mockTracing)
  }
}
