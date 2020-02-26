package io.opencensus.scala.akka.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.opencensus.scala.http.testSuite.MockStats
import io.opencensus.scala.stats.{Distribution, MeasurementDouble}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import Ordering.Double.TotalOrdering

class StatsDirectiveSpec
    extends AnyFlatSpec
    with ScalatestRouteTest
    with Matchers {

  def statsDirectiveWithMock: (StatsDirective, MockStats) = {
    val mockStats = new MockStats
    val directive = new StatsDirective {
      override private[http] val stats = mockStats
    }
    (directive, mockStats)
  }

  def routeWithMock = {
    val (directive, mock) = statsDirectiveWithMock

    val route = directive.recordRequest("routeName") {
      complete(StatusCodes.OK)
    }

    (route, mock)
  }

  it should "register the correct view" in {
    val (route, mock) = routeWithMock

    Get("/foo") ~> route ~> check {
      status shouldBe StatusCodes.OK

      mock.registeredViews should have length 1

      val serverLatency = mock.registeredViews.head

      serverLatency.name shouldBe "opencensus.io/http/server/server_latency"
      serverLatency.measure.name shouldBe "opencensus.io/http/server/server_latency"
      serverLatency.aggregation shouldBe a[Distribution]
    }
  }

  it should "record the correct measure value" in {
    val (route, mock) = routeWithMock

    Get("/foo") ~> route ~> check {
      status shouldBe StatusCodes.OK

      mock.recordedMeasurements should have length 1

      val (measurement, _) = mock.recordedMeasurements.head

      measurement match {
        case MeasurementDouble(measure, value) =>
          measure.name shouldBe "opencensus.io/http/server/server_latency"
          value shouldBe >=(0.0)
        case other => fail(s"Expected MeasurementDouble got $other")
      }
    }
  }

  it should "record the correct measure tags" in {
    val (route, mock) = routeWithMock

    Get("/foo") ~> route ~> check {
      status shouldBe StatusCodes.OK
      mock.recordedMeasurements should have length 1
      val (_, tags) = mock.recordedMeasurements.head

      val tagsKeyValues =
        tags.map(tag => (tag.key.getName, tag.value.asString()))

      val expectedTags = List(
        "http_server_method" -> "GET",
        "http_server_route"  -> "routeName",
        "http_server_status" -> "200"
      )

      tagsKeyValues should contain theSameElementsAs expectedTags
    }
  }
}
