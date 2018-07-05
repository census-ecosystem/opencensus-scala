package io.opencensus.scala.elastic4s

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{search => _, _}
import io.opencensus.scala.http.testSuite.MockTracing
import io.opencensus.trace.AttributeValue.{
  longAttributeValue,
  stringAttributeValue
}
import io.opencensus.trace.{BlankSpan, Span, Status}
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers, OptionValues}

import scala.concurrent.Future

class TracingHttpClientSpec
    extends AsyncFlatSpec
    with Matchers
    with OptionValues
    with EitherValues {

  behavior of "TracingHttpClient"

  it should "start and end a span when the request succeeds" in {
    val (mockTracing, client) = tracingAndClient(httpClient(), None)

    client.execute(search("testIndex")).map { _ =>
      mockTracing.startedSpans.headOption.value.name shouldBe "/testIndex/_search"
      mockTracing.endedSpansStatuses.headOption.value shouldBe Status.OK
    }
  }

  it should "Use the parent span if existing" in {
    val parentSpan            = BlankSpan.INSTANCE
    val (mockTracing, client) = tracingAndClient(httpClient(), Some(parentSpan))

    client.execute(search("testIndex")).map { _ =>
      mockTracing.startedSpans.headOption
        .flatMap(_.parentContext)
        .value shouldBe parentSpan.getContext
    }

  }

  it should "end a span when the request fails" in {
    val (mockTracing, client) =
      tracingAndClient(httpClient(Future.failed(new Exception("TEST"))), None)

    client.execute(search("testIndex")).failed.map { _ =>
      mockTracing.endedSpansStatuses.map(_.getCanonicalCode) should contain(
        Status.INTERNAL.getCanonicalCode
      )
    }
  }

  it should "return the result in case of success" in {
    val (_, client) = tracingAndClient(httpClient(), None)

    val emptySearchResponse =
      SearchResponse(0, false, false, null, null, None, null, null)
    client
      .execute(search("testIndex"))
      .map(_.right.value.result shouldBe emptySearchResponse)
  }

  it should "set the http attributes" in {
    val (mockTracing, client) = tracingAndClient(httpClient(), None)

    client.execute(search("testIndex")).map { _ =>
      val attributes = mockTracing.startedSpans.headOption.value.attributes

      attributes.get("http.path").value shouldBe stringAttributeValue(
        "/testIndex/_search"
      )
      attributes.get("http.method").value shouldBe stringAttributeValue("POST")
      attributes.get("http.status_code").value shouldBe longAttributeValue(200L)
      attributes.get("http.host").value shouldBe stringAttributeValue(
        "/elasticsearch"
      )
    }
  }

  val emptySearchRes: Future[HttpResponse] = Future.successful(
    HttpResponse(200, Some(HttpEntity.StringEntity("{}", None)), Map.empty)
  )

  private def httpClient(res: Future[HttpResponse] = emptySearchRes) =
    new HttpClient {
      override def client: HttpRequestClient = new HttpRequestClient {
        override def async(
            method: String,
            endpoint: String,
            params: Map[String, Any]
        ): Future[HttpResponse] =
          res

        override def async(
            method: String,
            endpoint: String,
            params: Map[String, Any],
            entity: HttpEntity
        ): Future[HttpResponse] =
          res

        override def close(): Unit = ()
      }

      override def close(): Unit = ()
    }

  private def tracingAndClient(c: HttpClient, parentSpan: Option[Span]) = {
    val tracing       = new MockTracing() {}
    val clientTracing = new TracingHttpClient(c, tracing, parentSpan)
    (tracing, clientTracing)
  }
}
