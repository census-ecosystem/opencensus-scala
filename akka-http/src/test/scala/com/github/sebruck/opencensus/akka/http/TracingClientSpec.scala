package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import com.github.sebruck.opencensus.Tracing
import io.opencensus.trace.{BlankSpan, Status}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class TracingClientSpec extends FlatSpec with Matchers with ScalaFutures {

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

    mockTracing.startedSpans should contain(
      ("/test", Some(BlankSpan.INSTANCE.getContext)))
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
