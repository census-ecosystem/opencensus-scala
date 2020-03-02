package io.opencensus.scala.akka.http

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.scala.Stats
import io.opencensus.scala.akka.http.stats.HttpStats
import io.opencensus.scala.akka.http.utils.ExecuteAfterResponse

import scala.concurrent.{ExecutionContext, Future}

trait StatsClient extends HttpStats with LazyLogging {

  /**
    *
    * @param doRequest the underlying function which should be instrumented. Usually comes from
    *                  `Http().singleRequest`.
    * @param routeName Should always be a low cardinality string
    *                  representing the logical route or handler of the request.
    *                  A reasonable interpretation of this would be the URL path pattern matched to
    *                  handle the request, or an explicitly specified function name.
    */
  def recorded(
      doRequest: HttpRequest => Future[HttpResponse],
      routeName: String
  )(
      implicit ec: ExecutionContext
  ): HttpRequest => Future[HttpResponse] = req => {
    val start = System.currentTimeMillis()

    doRequest(req).map(response =>
      ExecuteAfterResponse.onComplete(
        response,
        onFinish = () =>
          measureClientRoundtripLatency(
            routeName,
            req.method,
            response.status,
            (System.currentTimeMillis() - start).toDouble
          ).fold(
            error => logger.warn("Failed to measure server latency", error),
            identity
          ),
        onFailure = _ => ()
      )
    )
  }
}

object StatsClient extends StatsClient {
  override private[http] val stats = Stats
}
