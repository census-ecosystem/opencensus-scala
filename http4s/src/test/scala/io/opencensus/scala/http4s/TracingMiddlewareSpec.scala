package io.opencensus.scala.http4s

import cats.effect.IO
import io.opencensus.scala.http.ServiceData
import io.opencensus.scala.http4s.TracingService.{TracingService, withSpan}
import io.opencensus.trace.Span
import org.http4s._
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TracingMiddlewareSpec
    extends AnyFlatSpec
    with Matchers
    with OptionValues
    with ServiceRequirementsSpec {

  behavior of "TracingMiddleware"

  val ex   = new Exception("test exception")
  val data = ServiceData("serviceX", "x.s.2")
  def tracingService(
      response: Span => IO[Response[IO]] = span => Ok(span.getContext.toString)
  ): TracingService[IO] =
    TracingService[IO] {
      case GET -> Root / "my" / "fancy" / "path" withSpan span => response(span)
    }

  def service(response: IO[Response[IO]] = Ok()): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "my" / "fancy" / "path" => response
    }

  "tracing with span" should behave like testService(
    successServiceFromMiddleware = _.fromTracingService(tracingService()),
    failingServiceFromMiddleware =
      _.fromTracingService(tracingService(_ => IO.raiseError(ex))),
    badRequestServiceFromMiddleware =
      _.fromTracingService(tracingService(_ => BadRequest())),
    errorServiceFromMiddleware =
      _.fromTracingService(tracingService(_ => InternalServerError()))
  )

  "tracing with span and with service data" should behave like testService(
    successServiceFromMiddleware = _.fromTracingService(tracingService(), data),
    failingServiceFromMiddleware =
      _.fromTracingService(tracingService(_ => IO.raiseError(ex)), data),
    badRequestServiceFromMiddleware =
      _.fromTracingService(tracingService(_ => BadRequest()), data),
    errorServiceFromMiddleware =
      _.fromTracingService(tracingService(_ => InternalServerError()), data),
    Some(data)
  )

  it should "pass the span to the service" in {
    val (middleware, _) = middlewareWithMock()

    val responseBody = middleware
      .fromTracingService(tracingService())
      .run(request)
      .value
      .flatMap(_.get.as[String])
      .unsafeRunSync()

    responseBody should include("SpanContext")
  }

  "tracing without span" should behave like testService(
    successServiceFromMiddleware = _.withoutSpan(service()),
    failingServiceFromMiddleware = _.withoutSpan(service(IO.raiseError(ex))),
    badRequestServiceFromMiddleware = _.withoutSpan(service(BadRequest())),
    errorServiceFromMiddleware = _.withoutSpan(service(InternalServerError()))
  )

  "tracing without span and with service data" should behave like testService(
    successServiceFromMiddleware = _.withoutSpan(service(), data),
    failingServiceFromMiddleware =
      _.withoutSpan(service(IO.raiseError(ex)), data),
    badRequestServiceFromMiddleware = _.withoutSpan(service(BadRequest()), data),
    errorServiceFromMiddleware =
      _.withoutSpan(service(InternalServerError()), data),
    Some(data)
  )
}
