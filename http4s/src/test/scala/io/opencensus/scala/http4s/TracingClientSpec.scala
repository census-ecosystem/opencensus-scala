package io.opencensus.scala.http4s

import cats.effect.IO
import fs2.Stream
import io.opencensus.scala.Tracing
import io.opencensus.scala.http.propagation.Propagation
import io.opencensus.scala.http.testSuite.{MockPropagation, MockTracing}
import io.opencensus.trace.AttributeValue.{
  longAttributeValue,
  stringAttributeValue
}
import io.opencensus.trace.{BlankSpan, Status => CensusStatus}
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.scalatest._

import scala.util.Try
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TracingClientSpec
    extends AnyFlatSpec
    with Matchers
    with OptionValues
    with Http4sDsl[IO] {

  behavior of "TracingClient"

  val path = "my/fancy/path"
  def expectingClient(
      assertion: Request[IO] => Assertion = _ => Succeeded,
      response: IO[Response[IO]] = Ok()
  ): Client[IO] =
    Client.fromHttpApp[IO](
      HttpRoutes
        .of[IO] {
          case req @ GET -> Root / "my" / "fancy" / "path" =>
            assertion(req)
            response
        }
        .mapF(_.getOrElse(Response.notFound))
    )

  it should "start and end a span when the request succeeds" in {
    val (clientTracing, mockTracing) = clientTracingWithMock()

    clientTracing.trace(expectingClient()).expect[String](path).unsafeRunSync()
    mockTracing.startedSpans.headOption.value.name shouldBe path
    mockTracing.endedSpans.headOption
      .flatMap(_._2)
      .value shouldBe CensusStatus.OK
  }

  it should "set the status but not end the span if body is not drained" in {
    val (clientTracing, mockTracing) = clientTracingWithMock()

    val req = Request[IO](uri = Uri.unsafeFromString(path))
    clientTracing
      .trace(
        expectingClient(
          response =
            IO(Response(body = Stream("What".getBytes.toList: _*).covary[IO]))
        )
      ).run(req).use(_ => IO(""))
      .unsafeRunSync()

    mockTracing.spanStauts.headOption
      .map(_._2)
      .value shouldBe CensusStatus.OK
    mockTracing.endedSpans shouldBe empty
  }

  it should "Use the parent span if existing" in {
    val (clientTracing, mockTracing) = clientTracingWithMock()

    val parentSpan = BlankSpan.INSTANCE

    clientTracing
      .trace(expectingClient(), Some(parentSpan))
      .expect[String](path)
      .unsafeRunSync()

    mockTracing.startedSpans.headOption
      .flatMap(_.parentContext)
      .value shouldBe parentSpan.getContext
  }

  it should "enrich the HttpRequest with propagation headers" in {
    val (clientTracing, _) = clientTracingWithMock()

    val hasHeaderAssertion =
      (r: Request[IO]) =>
        r.headers.toList should contain(Header("X-Mock-Trace", "12345"))

    clientTracing
      .trace(expectingClient(hasHeaderAssertion))
      .expect[String](path)
      .unsafeRunSync()
  }

  it should "keep existing headers" in {
    val (clientTracing, _) = clientTracingWithMock()

    val testHeader = Header("Some", "Header")
    val request =
      Request[IO](
        uri = Uri.unsafeFromString(path),
        headers = Headers(List(testHeader))
      )

    val hasHeaderAssertion =
      (r: Request[IO]) => r.headers.toList should contain(testHeader)

    clientTracing
      .trace(expectingClient(hasHeaderAssertion))
      .expect[String](request)
      .unsafeRunSync()
  }

  it should "end a span when the request fails" in {
    val (clientTracing, mockTracing) = clientTracingWithMock()

    val err = IO.raiseError(new Error("TestException"))
    Try(
      clientTracing
        .trace(expectingClient(response = err))
        .expect[String](path)
        .unsafeRunSync()
    )

    mockTracing.endedSpans.map(_._2.get.getCanonicalCode) should contain(
      CensusStatus.INTERNAL.getCanonicalCode
    )
  }

  it should "return the http response in case of success" in {
    val (clientTracing, _) = clientTracingWithMock()

    val result = "RES"
    val response = clientTracing
      .trace(expectingClient(response = Ok(result)))
      .expect[String](path)
      .unsafeRunSync()
    response shouldBe result
  }

  it should "set the http attributes" in {
    val (clientTracing, mockTracing) = clientTracingWithMock()

    clientTracing
      .trace(expectingClient(), None)
      .expect[String]("http://example.com/" ++ path)
      .unsafeRunSync()

    val attributes = mockTracing.startedSpans.headOption.value.attributes

    attributes.get("http.path").value shouldBe stringAttributeValue(
      "/my/fancy/path"
    )
    attributes.get("http.method").value shouldBe stringAttributeValue("GET")
    attributes.get("http.status_code").value shouldBe longAttributeValue(200L)
    attributes.get("http.host").value shouldBe stringAttributeValue(
      "example.com"
    )
  }

  def clientTracingWithMock() = {
    val mockTracing = new MockTracing
    val clientTracing = new TracingClient[IO] {
      override protected val tracing: Tracing = mockTracing
      override protected val propagation: Propagation[Header, Request[IO]] =
        new MockPropagation[Header, Request[IO]] {
          override def rawHeader(key: String, value: String): Header =
            Header(key, value)
          override def path(request: Request[IO]): String =
            request.uri.path.toString
        }
    }
    (clientTracing, mockTracing)
  }
}
