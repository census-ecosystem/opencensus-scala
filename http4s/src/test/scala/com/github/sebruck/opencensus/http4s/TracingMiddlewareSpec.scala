package com.github.sebruck.opencensus.http4s

import cats.effect.IO
import com.github.sebruck.opencensus.http4s.TracingService.withSpan
import io.opencensus.trace.Span
import org.http4s._
import org.scalatest.{FlatSpec, Matchers, OptionValues}

class TracingMiddlewareSpec
    extends FlatSpec
    with Matchers
    with OptionValues
    with ServiceRequirementsSpec {

  behavior of "TracingMiddleware"

  val ex = new Exception("test exception")
  def tracingService(response: Span => IO[Response[IO]] = span =>
    Ok(span.getContext.toString)) =
    TracingService[IO] {
      case GET -> Root / "my" / "fancy" / "path" withSpan span => response(span)
    }

  def service(response: IO[Response[IO]] = Ok()) =
    HttpService[IO] {
      case GET -> Root / "my" / "fancy" / "path" => response
    }

  "tracing with span" should behave like testService(
    successServiceFromMiddleware = _(tracingService()),
    failingServiceFromMiddleware = _(tracingService(_ => IO.raiseError(ex))),
    badRequestServiceFromMiddleware = _(tracingService(_ => BadRequest())),
    errorServiceFromMiddleware = _(tracingService(_ => InternalServerError()))
  )

  it should "pass the span to the service" in {
    val (middleware, _) = middlewareWithMock()

    val responseBody = middleware(tracingService())
      .orNotFound(request)
      .flatMap(_.as[String])
      .unsafeRunSync()

    responseBody should include("SpanContext")
  }

  "tracing without span" should behave like testService(
    successServiceFromMiddleware = _.withoutSpan(service()),
    failingServiceFromMiddleware = _.withoutSpan(service(IO.raiseError(ex))),
    badRequestServiceFromMiddleware = _.withoutSpan(service(BadRequest())),
    errorServiceFromMiddleware = _.withoutSpan(service(InternalServerError()))
  )
}
