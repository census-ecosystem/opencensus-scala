package io.opencensus.scala.akka.http

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.Materializer
import io.opencensus.scala.http.testSuite.MockTracing
import io.opencensus.trace.{BlankSpan, Span, SpanContext, Status}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

trait ClientSpec
    extends AsyncFlatSpec
    with Matchers
    with ScalaFutures
    with OptionValues {

  type Client = (HttpRequest => Future[HttpResponse],
                 Span) => HttpRequest => Future[HttpResponse]

  def testClient(clientWithMock: () => (Client, MockTracing),
                 withParent: Boolean)(implicit mat: Materializer): Unit = {

    it should "enrich the HttpRequest with propagation headers" in {
      val (client, _) = clientWithMock()

      client({ request: HttpRequest =>
        request.headers should contain(RawHeader("X-Mock-Trace", "12345"))
        Future.successful(HttpResponse())
      }, BlankSpan.INSTANCE)(HttpRequest())
        .map(_ => Succeeded)
    }

    it should "keep existing headers" in {
      val (client, _) = clientWithMock()
      val header      = RawHeader("Foo", "Bar")
      val request     = HttpRequest(headers = List(RawHeader("Foo", "Bar")))

      client(
        { request =>
          request.headers should contain(RawHeader("X-Mock-Trace", "12345"))
          request.headers should contain(header)
          Future.successful(HttpResponse())
        },
        BlankSpan.INSTANCE
      )(request).map(_ => Succeeded)
    }

    it should "start and end a span when the request succeeds" in {
      val (client, mockTracing) = clientWithMock()

      val call = client(_ => Future.successful(HttpResponse()),
                        BlankSpan.INSTANCE)(HttpRequest(uri = "/test"))
        .flatMap(_.discardEntityBytes().future())

      call.map { _ =>
        val startedSpan = mockTracing.startedSpans.headOption.value

        startedSpan.name shouldBe "/test"

        if (withParent)
          startedSpan.parentContext.value shouldBe SpanContext.INVALID
        else
          startedSpan.parentContext shouldBe None

        mockTracing.endedSpansStatuses should contain(Status.OK)
      }
    }

    it should "end a span when the request fails" in {
      val (client, mockTracing) = clientWithMock()

      val failingClient = recoverToSucceededIf[Exception](
        client(_ => Future.failed(new Exception("Test Error")),
               BlankSpan.INSTANCE)(HttpRequest(uri = "/test")))

      failingClient.map { _ =>
        mockTracing.endedSpansStatuses.map(_.getCanonicalCode) should contain(
          Status.INTERNAL.getCanonicalCode)
      }
    }

    it should "add an annotation for receiving the response" in {
      val (client, mockTracing) = clientWithMock()

      client(_ => Future.successful(HttpResponse()), BlankSpan.INSTANCE)(
        HttpRequest(uri = "/test")).map { _ =>
        val startedSpan = mockTracing.startedSpans.headOption.value

        startedSpan.annotaions should contain("response received")
      }
    }

    it should "return the http response in case of success" in {
      val (client, _) = clientWithMock()

      val result =
        client(_ => Future.successful(HttpResponse(StatusCodes.ImATeapot)),
               BlankSpan.INSTANCE)(HttpRequest(uri = "/test"))

      result.map(_.status shouldBe StatusCodes.ImATeapot)
    }

    it should "return the exception in case of failure" in {
      val (client, _) = clientWithMock()

      val result = client(_ => Future.failed(new Exception("Test error")),
                          BlankSpan.INSTANCE)(HttpRequest(uri = "/test")).failed

      result.map(_.getMessage shouldBe "Test error")
    }

    it should "set the http attributes" in {
      import io.opencensus.trace.AttributeValue._

      val (client, mock) = clientWithMock()
      val request        = HttpRequest(uri = "http://example.com/my/fancy/path")

      client(_ => Future.successful(HttpResponse()), BlankSpan.INSTANCE)(
        request).map { _ =>
        // Wait some time to ensure the "onComplete" which writes the status_code attribute has run
        Thread.sleep(100)
        val attributes = mock.startedSpans.headOption.value.attributes

        attributes.get("http.host").value shouldBe stringAttributeValue(
          "example.com")
        attributes.get("http.path").value shouldBe stringAttributeValue(
          "/my/fancy/path")
        attributes.get("http.method").value shouldBe stringAttributeValue("GET")
        attributes.get("http.status_code").value shouldBe longAttributeValue(
          200L)
      }
    }
  }
}
