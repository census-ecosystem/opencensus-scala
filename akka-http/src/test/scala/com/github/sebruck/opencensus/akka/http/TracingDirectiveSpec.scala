package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.sebruck.opencensus.Tracing
import io.opencensus.trace.Status
import org.scalatest.{FlatSpec, Matchers}

class TracingDirectiveSpec
    extends FlatSpec
    with Matchers
    with ScalatestRouteTest {

  behavior of "trace"

  it should "start a span with the path of the request" in {
    val (directive, mockTracing) = directiveWithMock()

    val path = "/my/fancy/path"
    Get(path) ~> directive.traceRequest(_ => Directives.complete("")) ~> check {
      mockTracing.startedSpans should contain(path)
    }
  }

  it should "end a span with status OK when the route is successfully" in {
    val (directive, mockTracing) = directiveWithMock()

    val path = "/my/fancy/path"
    Get(path) ~> directive.traceRequest(_ => Directives.complete("")) ~> check {
      mockTracing.endedSpansStatuses should contain(Status.OK)
    }
  }

  it should "end a span with status INTERNAL when the route fails" in {
    val (directive, mockTracing) = directiveWithMock()

    val path = "/my/fancy/path"
    Get(path) ~> directive.traceRequest(_ =>
      throw new Exception("test exception")) ~> check {
      mockTracing.endedSpansStatuses should contain(Status.INTERNAL)
    }
  }

  def directiveWithMock() = {
    val mockTracing = new MockTracing
    val directive = new TracingDirective {
      override protected def tracing: Tracing = mockTracing
    }

    (directive, mockTracing)
  }
}
