package io.opencensus.scala.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import io.opencensus.scala.http.testSuite.MockStats
import io.opencensus.scala.stats.{Distribution, MeasurementDouble}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class StatsClientSpec
    extends AsyncFlatSpec
    with BeforeAndAfterAll
    with Matchers {
  implicit val system: ActorSystem = ActorSystem()

  def statsClientWithMock: (StatsClient, MockStats) = {
    val mockStats = new MockStats
    val client = new StatsClient {
      override private[http] val stats = mockStats
    }

    (client, mockStats)
  }

  it should "register the correct view" in {
    val (client, mock) = statsClientWithMock

    val doRequest =
      client.recorded(_ => Future.successful(HttpResponse()), "routeName")

    doRequest(HttpRequest()).flatMap(_.discardEntityBytes().future()).map { _ =>
      mock.registeredViews should have length 1

      val roundtripLatency = mock.registeredViews.head

      roundtripLatency.name shouldBe "opencensus.io/http/client/roundtrip_latency"
      roundtripLatency.measure.name shouldBe "opencensus.io/http/client/roundtrip_latency"
      roundtripLatency.aggregation shouldBe a[Distribution]
    }
  }
  it should "record the correct measure value" in {
    val (client, mock) = statsClientWithMock

    val doRequest =
      client.recorded(_ => Future.successful(HttpResponse()), "routeName")

    doRequest(HttpRequest()).flatMap(_.discardEntityBytes().future()).map { _ =>
      val (measurement, _) = mock.recordedMeasurements.head

      measurement match {
        case MeasurementDouble(measure, value) =>
          measure.name shouldBe "opencensus.io/http/client/roundtrip_latency"
          value.toInt shouldBe >(0)
        case other => fail(s"Expected MeasurementDouble got $other")
      }
    }
  }

  it should "record the correct measure tags" in {
    val (client, mock) = statsClientWithMock

    val doRequest =
      client.recorded(_ => Future.successful(HttpResponse()), "routeName")

    doRequest(HttpRequest()).flatMap(_.discardEntityBytes().future()).map { _ =>
      mock.recordedMeasurements should have length 1
      val (_, tags) = mock.recordedMeasurements.head

      val tagsKeyValues =
        tags.map(tag => (tag.key.getName, tag.value.asString()))

      val expectedTags = List(
        "http_client_method" -> "GET",
        "http_client_route"  -> "routeName",
        "http_client_status" -> "200"
      )

      tagsKeyValues should contain theSameElementsAs expectedTags
    }
  }

  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
}
