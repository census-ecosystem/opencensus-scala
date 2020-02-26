# opencensus Akka HTTP instrumentation
This modules contains utilities to use opencensus in Akka HTTP applications. It supports:

* [Tracing](#quickstart-tracing) for the server and the client
* [Stats](#quickstart-stats) for the server

The API documentation can be found [here](https://census-ecosystem.github.io/opencensus-scala/api/).

## Quickstart Tracing
Have a look at the [usage examples](../akka-http-example/src/main/scala/com/github/sebruck/opencensus/examples/akka/http).

In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-akka-http" % "0.7.0"

// Dependent on the trace exporters you want to use add one or more of the following
"io.opencensus" % "opencensus-exporter-trace-stackdriver" % "0.23.0"
"io.opencensus" % "opencensus-exporter-trace-logging"     % "0.23.0"
"io.opencensus" % "opencensus-exporter-trace-instana"     % "0.23.0"
"io.opencensus" % "opencensus-exporter-trace-zipkin"      % "0.23.0"
```

To enable the Stackdriver trace exporter add the following to your typesafe config file:
```
opencensus-scala {
  trace {
    // Be carefull, this will sample 100% of your traces
    sampling-probability = 1,
    exporters {
      stackdriver {
        enabled = true 
        project-id = "MY-GC-Project"
      }
    }
  }
}
```

### Server
```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

object TracingService extends App {
  import io.opencensus.scala.akka.http.TracingDirective._

  implicit val system: ActorSystem             = ActorSystem()
  import system.dispatcher

  val route = get {
    traceRequest { span =>
      complete("Traced span with context: " + span.getContext)
    }
  }

  Http().bindAndHandle(route, "0.0.0.0", port = 8080)
}
```

The `traceRequest` directive starts a new span and sets the span context which got propagated in 
the [B3 Format](https://github.com/openzipkin/b3-propagation#overall-process). If no or invalid B3 headers
are present it will start a new root span. 

When the request completes or fails the span is ended with a proper status which fits to the http response code.

### Client

#### Warning - Drain the response entity
As stated in the akka [docs](https://doc.akka.io/docs/akka-http/current/implications-of-streaming-http-entity.html#implications-of-the-streaming-nature-of-request-response-entities)
>Consuming (or discarding) the Entity of a request is mandatory!

Otherwise a `Span` might not be closed.

#### Request-Level client
```scala
val response: Future[HttpResponse] = TracingClient.traceRequest(Http().singleRequest(_), parentSpan)(HttpRequest())
```

#### Host-Level client
```scala
  val flow: Flow[(HttpRequest, T), (Try[HttpResponse], T), NotUsed] = 
      TracingClient.traceRequestForPool(Http().cachedHostConnectionPool[T]("host"), parentSpan)
```

#### Connection-Level client
```scala
  val flow: Flow[HttpRequest, HttpResponse, NotUsed] = TracingClient.traceRequestForConnection(Http().outgoingConnection("host"), parentSpan)
```

The `traceRequest` function enriches the given function with type `HttpRequest => Future[HttpResponse]` and wraps the
call in a span. Additionally the `HttpRequest` gets enriched with headers in the 
[B3 Format](https://github.com/openzipkin/b3-propagation#overall-process).

When the call completes or fails the span is ended with a proper status which fits to the http response code.

## Quickstart Stats 

In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-akka-http" % "0.7.2"

// Dependent on the stats exporters you want to use add one or more of the following
"io.opencensus" % "opencensus-exporter-stats-stackdriver" % "0.23.0"
"io.opencensus" % "opencensus-exporter-stats-prometheus"  % "0.23.0"
"io.opencensus" % "opencensus-exporter-stats-signalfx"    % "0.23.0"

// To run this (prometheus) example
"io.prometheus" % "simpleclient_httpserver" % "0.8.1"
```

### Server & Client
```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector
import io.opencensus.scala.akka.http.StatsClient
import io.opencensus.scala.akka.http.StatsDirective.recordRequest

object Test extends App {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  // Initialize Prometheus
  PrometheusStatsCollector.createAndRegister()
  new io.prometheus.client.exporter.HTTPServer(8081)

  val route = get {
    // record stats on the server
    recordRequest("logical-route-name") {
      complete("stats-recorder")
    }
  }

  // record stats from the client perspective
  val singleRequestWithStats = StatsClient.recorded(Http().singleRequest(_), _)

  for {
    _ <- Http().bindAndHandle(route, "0.0.0.0", port = 8080)
    _ <- singleRequestWithStats("logical-route-name")(HttpRequest(uri = "http://localhost:8080/"))
      .flatMap(_.discardEntityBytes().future())
  } yield ()
}
```

Now run:
```
$ curl -XGET http://localhost:8081/
# HELP opencensus_io_http_server_server_latency Time between first byte of request headers read to last byte of response sent, or terminal error
# TYPE opencensus_io_http_server_server_latency histogram
...
opencensus_io_http_server_server_latency_bucket{http_server_method="GET",http_server_route="logical-route-name",http_server_status="200",le="13.0",} 0.0
opencensus_io_http_server_server_latency_bucket{http_server_method="GET",http_server_route="logical-route-name",http_server_status="200",le="16.0",} 0.0
opencensus_io_http_server_server_latency_bucket{http_server_method="GET",http_server_route="logical-route-name",http_server_status="200",le="20.0",} 0.0
...
# HELP opencensus_io_http_client_roundtrip_latency Time between first byte of request headers sent to last byte of response received, or terminal error
# TYPE opencensus_io_http_client_roundtrip_latency histogram
opencensus_io_http_client_roundtrip_latency_bucket{http_client_method="GET",http_client_route="logical-route-name",http_client_status="200",le="13.0",} 0.0
opencensus_io_http_client_roundtrip_latency_bucket{http_client_method="GET",http_client_route="logical-route-name",http_client_status="200",le="16.0",} 0.0
opencensus_io_http_client_roundtrip_latency_bucket{http_client_method="GET",http_client_route="logical-route-name",http_client_status="200",le="20.0",} 0.0
...
```

## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)
