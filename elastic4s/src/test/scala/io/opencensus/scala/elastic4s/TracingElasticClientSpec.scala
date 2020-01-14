package io.opencensus.scala.elastic4s

import cats.Id
import com.sksamuel.elastic4s.{
  ElasticClient,
  ElasticRequest,
  Executor,
  Functor,
  HttpClient,
  HttpEntity,
  HttpResponse
}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.opencensus.scala.http.testSuite.MockTracing
import io.opencensus.trace.AttributeValue.{
  longAttributeValue,
  stringAttributeValue
}
import io.opencensus.trace.{BlankSpan, Span, Status}
import org.scalatest._

import scala.util.Try
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TracingElasticClientSpec
    extends AnyFlatSpec
    with Matchers
    with OptionValues
    with EitherValues {

  behavior of "TracingElasticClient"

  it should "start and end a span when the request succeeds" in {
    val (mockTracing, syncClient) = tracingAndClient(elasticClient(), None)

    syncClient.execute(search("testIndex"))
    mockTracing.startedSpans.headOption.value.name shouldBe "/testIndex/_search"
    mockTracing.endedSpans.headOption.flatMap(_._2).value shouldBe Status.OK
  }

  it should "Use the parent span if existing" in {
    val parentSpan = BlankSpan.INSTANCE
    val (mockTracing, syncClient) =
      tracingAndClient(elasticClient(), Some(parentSpan))

    syncClient.execute(search("testIndex"))

    mockTracing.startedSpans.headOption
      .flatMap(_.parentContext)
      .value shouldBe parentSpan.getContext
  }

  it should "end a span when the request fails" in {
    val (mockTracing, syncClient) =
      tracingAndClient(elasticClient(Left(new Exception("TEST"))), None)

    Try(syncClient.execute(search("testIndex")))

    mockTracing.endedSpans.map(_._2.get.getCanonicalCode) should contain(
      Status.INTERNAL.getCanonicalCode
    )
  }

  it should "return the result in case of success" in {
    val (_, syncClient) = tracingAndClient(elasticClient(), None)

    val emptySearchResponse =
      SearchResponse(0, false, false, null, null, None, null, null)

    syncClient
      .execute(search("testIndex"))
      .result shouldBe emptySearchResponse
  }

  it should "set the http attributes" in {
    val (mockTracing, syncClient) = tracingAndClient(elasticClient(), None)

    syncClient.execute(search("testIndex"))
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

  val emptySearchRes: Either[Throwable, HttpResponse] =
    Right(
      HttpResponse(200, Some(HttpEntity.StringEntity("{}", None)), Map.empty)
    )

  private def elasticClient(
      res: Either[Throwable, HttpResponse] = emptySearchRes
  ) =
    ElasticClient(new HttpClient {
      override def send(
          request: ElasticRequest,
          callback: Either[Throwable, HttpResponse] => Unit
      ): Unit =
        callback(res)

      override def close(): Unit = ()
    })

  private def tracingAndClient(c: ElasticClient, parentSpan: Option[Span]) = {
    val tracing = new MockTracing() {}
    val clientTracing =
      TracingElasticClient.tracingElasticClient(c, tracing, parentSpan)
    (tracing, clientTracing)
  }

  implicit val executorId: Executor[Id] =
    (client: HttpClient, request: ElasticRequest) => {
      var res: Option[HttpResponse] = None
      client.send(request, in => in.fold(throw _, r => res = Some(r)))
      while (res.isEmpty) {
        // waiting for client
      }
      res.get
    }

  implicit val funF: Functor[Id] = new Functor[Id] {
    override def map[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)
  }
}
