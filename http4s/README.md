# opencensus Http4s instrumentation
This modules contains utilities to use opencensus in Http4s applications.

The API documentation can be found [here](https://sebruck.github.io/opencensus-scala/).

## Quickstart
In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-http4s" % "0.3.0" 

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

### Server


The `TracingMiddleware` starts a new span and sets the span context which got propagated in 
the [B3 Format](https://github.com/openzipkin/b3-propagation#overall-process). If no or invalid B3 headers
are present it will start a new root span. 

When the request completes or fails the span is ended with a proper status which fits to the http response code.


#### Tracing and passing span to an underlying service
```scala
import cats.effect.IO
import com.github.sebruck.opencensus.http4s.TracingService._
import com.github.sebruck.opencensus.http4s.TracingMiddleware
import org.http4s._
import org.http4s.dsl.Http4sDsl


object TracingService extends Http4sDsl[IO] {
  val service: TracingService[IO] = TracingService[IO] {
     case GET -> Root / "path" withSpan span =>
       Ok(span.getContext.toString)
   }
 
   val withTracingMiddleware: HttpService[IO] = TracingMiddleware(service)
}
```

#### Tracing and not passing span to an underlying service

```scala
import cats.effect.IO
import com.github.sebruck.opencensus.http4s.TracingService._
import com.github.sebruck.opencensus.http4s.TracingMiddleware
import org.http4s._
import org.http4s.dsl.Http4sDsl


object TracingService extends Http4sDsl[IO] {
  val service: HttpService[IO] = HttpService[IO] {
    case GET -> Root / "path" =>
      Ok()
  }

  val withTracingMiddleware: HttpService[IO] =
    TracingMiddleware.withoutSpan(service)
}

```

## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)
