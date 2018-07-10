# opencensus-scala core
This module provides utilities to use opencensus in a scala idiomatic way.

The API documentation can be found [here](https://census-ecosystem.github.io/opencensus-scala/api/).

## Tracing Quickstart

The [Tracing](https://census-ecosystem.github.io/opencensus-scala/api/api/com/github/sebruck/opencensus/Tracing$.html) type 
is the entry point of the library.

In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-core" % "0.5.0" 

// Dependent on the trace exporters you want to use add one or more of the following
"io.opencensus" % "opencensus-exporter-trace-stackdriver" % "0.13.2"
"io.opencensus" % "opencensus-exporter-trace-logging"     % "0.13.2"
"io.opencensus" % "opencensus-exporter-trace-instana"     % "0.13.2"
"io.opencensus" % "opencensus-exporter-trace-zipkin"      % "0.13.2"
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

````scala
import io.opencensus.trace.Status

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object AsyncTracingApp extends App {
  import Tracing._

  // functional style with a higher order function
  // the span will automatically be ended when the future completes
  def aFunction(): Future[Int]          = Future(Int.MaxValue)
  def anotherFunction(): Future[String] = Future("Hello!")

  val theResult: Future[Int] = trace("A large int") { span =>
    traceChild("a child span", parentSpan = span)(_ => anotherFunction())
    aFunction()
  }

  // imperative style with explicit start and end
  val span = startSpan("Another large int")
  aFunction().onComplete {
    case Success(_) => endSpan(span, Status.OK)
    case Failure(_) => endSpan(span, Status.UNKNOWN)
  }

  // imperative style in synchronous code
  val anotherSpan = startSpan("Again a span")
  // ... do something
  Thread.sleep(2000)
  endSpan(anotherSpan, Status.OK)
}
````

## Stats Quickstart
The [Stats](https://census-ecosystem.github.io/opencensus-scala/api/api/com/github/sebruck/opencensus/Stats$.html) type 
is the entry point of the library.

In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-core" % "0.5.0" 

// Dependent on the trace exporters you want to use add one or more of the following
"io.opencensus" % "opencensus-exporter-stats-stackdriver" % "0.13.2"
"io.opencensus" % "opencensus-exporter-stats-prometheus"  % "0.13.2"
"io.opencensus" % "opencensus-exporter-stats-signalfx"    % "0.13.2"

// To run this (prometheus) example
"io.prometheus" % "simpleclient_httpserver" % "0.4.0"
```

```scala
import com.github.sebruck.opencensus.Stats
import com.github.sebruck.opencensus.stats._
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector

object Example extends App {

  /*
   Register Prometheus stats collector
   and start the prometheus exposition http server
   */
  PrometheusStatsCollector.createAndRegister()
  val server = new io.prometheus.client.exporter.HTTPServer(8080)

  /*
   Has to be done once for each measure and view
   */
  val ServiceTagName = "service_name"
  val measureOrFailure = for {
    // Create a new measure
    measure <- Measure.double("my_measure", "what a cool measure", "by")
    // Create & register a view which aggregates this measure
    view <- View("my_view",
                 "view description",
                 measure,
                 List(ServiceTagName),
                 Sum)
    _ <- Stats.registerView(view)
  } yield measure

  // throws exception if something above failed, should be handled properly
  val measure = measureOrFailure.get

  /*
   Has to be done each time when a new value should be recorded
   */
  // throws exception if something above failed, should be handled properly
  val tag = Tag(ServiceTagName, "my fancy service").get
  Stats.record(List(tag), Measurement.double(measure, 2.1))
}
```

If we now run this and do a curl, we can see the recorded metrics.

```
$ curl -XGET http://localhost:8080/metrics
# HELP my_view view description
# TYPE my_view untyped
opencensus_my_view{service_name="my fancy service",} 2.1
```

## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)