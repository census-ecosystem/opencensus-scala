package io.opencensus.scala.akka.http

import akka.http.scaladsl.server.Directives
import io.opencensus.scala.http.ServiceData

class TracingDirectiveSpec extends ServiceRequirementsSpec {

  "traceRequest" should behave like tracedService(
    routeFromTracingDirectiveAndResult =
      directive => completeWith => directive.traceRequest(_ => completeWith()),
    serviceData = None
  )

  it should "pass the span to the inner route" in {
    val path           = "/my/fancy/path"
    val (directive, _) = directiveWithMock()
    Get(path) ~> directive.traceRequest(span =>
      Directives.complete(span.getContext.toString)
    ) ~> check {
      entityAs[String] should include("SpanContext")
    }
  }

  "traceRequestNoSpan" should behave like tracedService(
    routeFromTracingDirectiveAndResult =
      directive => completeWith => directive.traceRequestNoSpan(completeWith()),
    serviceData = None
  )

  val data = ServiceData("Name", "X.X.Y")
  "traceRequestForService" should behave like tracedService(
    routeFromTracingDirectiveAndResult = directive =>
      completeWith =>
        directive.traceRequestForService(data)(_ => completeWith()),
    serviceData = Some(data)
  )
  "traceRequestForServiceNoSpan" should behave like tracedService(
    routeFromTracingDirectiveAndResult = directive =>
      completeWith =>
        directive.traceRequestForServiceNoSpan(data)(completeWith()),
    serviceData = Some(data)
  )
}
