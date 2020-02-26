package io.opencensus.scala.akka.http.stats

import akka.http.scaladsl.model.{HttpMethod, StatusCode}
import io.opencensus.scala.Stats
import io.opencensus.scala.stats._

private[http] trait HttpStats {
  private[http] def stats: Stats

  // As defined in https://github.com/census-instrumentation/opencensus-specs/blob/master/stats/HTTP.md
  private val latencyBucketBoundaries = List(0, 1, 2, 3, 4, 5, 6, 8, 10, 13, 16,
    20, 25, 30, 40, 50, 65, 80, 100, 130, 160, 200, 250, 300, 400, 500, 650,
    800, 1000, 2000, 5000, 10000, 20000, 50000, 100000).map(_.toDouble)

  private val MethodTagName = "http_server_method"
  private val RouteTagName  = "http_server_route"
  private val StatusTagName = "http_server_status"

  private lazy val serverLatency = (for {
    measure <- Measure.double(
      name = "opencensus.io/http/server/server_latency",
      description =
        "Time between first byte of request headers read to last byte of response sent, or terminal error",
      unit = "ms"
    )
    distribution <- Distribution(latencyBucketBoundaries)
    view <- View(
      name = "opencensus.io/http/server/server_latency",
      description =
        "Time between first byte of request headers read to last byte of response sent, or terminal error",
      measure,
      List(MethodTagName, RouteTagName, StatusTagName),
      distribution
    )
    _ <- stats.registerView(view)
  } yield measure)

  private[http] def measureServerLatency(
      route: String,
      method: HttpMethod,
      status: StatusCode,
      durationInMs: Double
  ) =
    for {
      measure <- serverLatency
      tags <- Tag(
        MethodTagName -> method.value,
        RouteTagName  -> route,
        StatusTagName -> status.intValue.toString
      )
    } yield stats.record(tags, Measurement.double(measure, durationInMs))

}
