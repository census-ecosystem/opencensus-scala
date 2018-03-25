# opencensus-scala core
This module provides utilities to use opencensus in a scala idiomatic way.

## Quickstart

The [Tracing](src/main/scala/com/github/sebruck/opencensus/Tracing.scala) type is the entry point
of the library. (TODO: Add link to scala doc once it gets generated)

In your build.sbt add the following dependency:

```scala
// TODO: Add version once the first version is published
"com.github.sebruck" %% "opencensus-scala-core" % "0.1.1" 
```

To enable the Stackdriver trace exporter add the following to your typesafe config file:
```
opencensus-scala {
    // Be carefull, this will sample 100% of your traces
    sampling-probability = 1,
    stackdriver {
        enabled = true
        project-id = "MY-GC-Project"
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