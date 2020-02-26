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

  private val ServerMethodTagName = "http_server_method"
  private val ServerRouteTagName  = "http_server_route"
  private val ServerStatusTagName = "http_server_status"

  private val ClientMethodTagName = "http_client_method"
  private val ClientRouteTagName  = "http_client_route"
  private val ClientStatusTagName = "http_client_status"

  private lazy val serverLatency = for {
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
      List(ServerMethodTagName, ServerRouteTagName, ServerStatusTagName),
      distribution
    )
    _ <- stats.registerView(view)
  } yield measure

  private lazy val clientRoundTripLatency =
    for {
      measure <- Measure.double(
        name = "opencensus.io/http/client/roundtrip_latency",
        description =
          "Time between first byte of request headers sent to last byte of response received, or terminal error",
        unit = "ms"
      )
      distribution <- Distribution(latencyBucketBoundaries)
      view <- View(
        name = "opencensus.io/http/client/roundtrip_latency",
        description =
          "Time between first byte of request headers sent to last byte of response received, or terminal error",
        measure,
        List(ClientMethodTagName, ClientRouteTagName, ClientStatusTagName),
        distribution
      )
      _ <- stats.registerView(view)
    } yield measure

  private[http] def measureClientRoundtripLatency(
      route: String,
      method: HttpMethod,
      status: StatusCode,
      durationInMs: Double
  ) =
    for {
      measure <- clientRoundTripLatency
      tags <- Tag(
        ClientMethodTagName -> method.value,
        ClientRouteTagName  -> route,
        ClientStatusTagName -> status.intValue.toString
      )
    } yield stats.record(tags, Measurement.double(measure, durationInMs))

  private[http] def measureServerLatency(
      route: String,
      method: HttpMethod,
      status: StatusCode,
      durationInMs: Double
  ) =
    for {
      measure <- serverLatency
      tags <- Tag(
        ServerMethodTagName -> method.value,
        ServerRouteTagName  -> route,
        ServerStatusTagName -> status.intValue.toString
      )
    } yield stats.record(tags, Measurement.double(measure, durationInMs))

}
