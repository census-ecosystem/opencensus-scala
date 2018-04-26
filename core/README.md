# opencensus-scala core
This module provides utilities to use opencensus in a scala idiomatic way.

The API documentation can be found [here](https://sebruck.github.io/opencensus-scala/).

## Quickstart

The [Tracing](https://sebruck.github.io/opencensus-scala/api/com/github/sebruck/opencensus/Tracing$.html) type 
is the entry point of the library.

In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-core" % "0.4.0" 

// Dependent on the trace exporters you want to use add one or more of the following
"io.opencensus" % "opencensus-exporter-trace-stackdriver" % "0.12.3"
"io.opencensus" % "opencensus-exporter-trace-logging"     % "0.12.3"
"io.opencensus" % "opencensus-exporter-trace-instana"     % "0.12.3"
"io.opencensus" % "opencensus-exporter-trace-zipkin"      % "0.12.3"
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

## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)