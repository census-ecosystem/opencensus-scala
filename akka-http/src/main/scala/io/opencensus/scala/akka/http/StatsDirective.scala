package io.opencensus.scala.akka.http

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.scala.Stats
import io.opencensus.scala.akka.http.stats.HttpStats
import io.opencensus.scala.akka.http.utils.ExecuteAfterResponse

trait StatsDirective extends HttpStats with LazyLogging {
  override private[http] def stats: Stats

  /**
    * Records the handled http requests as defined in https://github.com/census-instrumentation/opencensus-specs/blob/master/stats/HTTP.md
    *
    * @param routeName Should always be a low cardinality string
    *                  representing the logical route or handler of the request.
    *                  A reasonable interpretation of this would be the URL path pattern matched to
    *                  handle the request, or an explicitly specified function name.
    */
  def recordRequest(routeName: String): Directive0 =
    extractRequest.flatMap { req =>
      val startTime = System.currentTimeMillis()

      record(req, routeName, startTime)
    }

  private def record(req: HttpRequest, routeName: String, startTime: Long) =
    mapResponse(res =>
      ExecuteAfterResponse
        .onComplete(
          res,
          onFinish = () =>
            measureServerLatency(
              routeName,
              req.method,
              res.status,
              (System.currentTimeMillis() - startTime).toDouble
            ).fold(
              error => logger.warn("Failed to measure server latency", error),
              identity
            ),
          onFailure = _ => ()
        )
    )
}

object StatsDirective extends StatsDirective {
  override private[http] val stats = Stats
}
