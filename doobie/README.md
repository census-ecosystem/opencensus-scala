# opencensus doobie instrumentation
This modules contains utilities to use opencensus in doobie applications.

The API documentation can be found [here](https://census-ecosystem.github.io/opencensus-scala/api/).

## Quickstart
In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-doobie" % "0.7.2"

// Dependent on the trace exporters you want to use add one or more of the following
"io.opencensus" % "opencensus-exporter-trace-stackdriver" % "0.25.0"
"io.opencensus" % "opencensus-exporter-trace-logging"     % "0.25.0"
"io.opencensus" % "opencensus-exporter-trace-instana"     % "0.25.0"
"io.opencensus" % "opencensus-exporter-trace-zipkin"      % "0.25.0"
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

### Tracing Connections

```scala
  import doobie.implicits._
  import io.opencensus.scala.doobie.implicits._
  import cats.effect.IO
  import doobie.util.transactor.Transactor
  import io.opencensus.trace.Span

  val xa: Transactor[IO] = ???

  // simple selection
  sql"SELECT 42".query[Int].unique.tracedTransact(xa, "42 transaction")

  // with parent span
  val parent: Span = ???
  sql"SELECT 42".query[Int].unique.tracedTransact(xa, "42 transaction", parent)
```

Stream based execution of queries is not supported by the tracing.

## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)
