# opencensus Http4s instrumentation
This modules contains utilities to use opencensus in Http4s applications.

The API documentation can be found [here](https://census-ecosystem.github.io/opencensus-scala/api/).

## Quickstart
In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-http4s" % "0.7.0-M1" 

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


The `TracingMiddleware` starts a new span and sets the span context which got propagated in 
the [B3 Format](https://github.com/openzipkin/b3-propagation#overall-process). If no or invalid B3 headers
are present it will start a new root span. 

When the request completes or fails the span is ended with a proper status which fits to the http response code.


#### Tracing and passing span to an underlying service
```scala
import cats.effect.IO
import io.opencensus.scala.http4s.TracingService._
import io.opencensus.scala.http4s.TracingMiddleware
import io.opencensus.scala.http.ServiceData
import org.http4s._
import org.http4s.dsl.Http4sDsl


object TracingService extends Http4sDsl[IO] {
  val service: TracingService[IO] = TracingService[IO] {
     case GET -> Root / "path" withSpan span =>
       Ok(span.getContext.toString)
   }
 
   // optional service data gets added as attribute to the created span
   val serviceData = ServiceData(name = "MyService", version = "1.2.3")
   val withTracingMiddleware: HttpService[IO] = TracingMiddleware(service, serviceData)
}
```

#### Tracing and not passing span to an underlying service

```scala
import cats.effect.IO
import io.opencensus.scala.http4s.TracingService._
import io.opencensus.scala.http4s.TracingMiddleware
import org.http4s._
import org.http4s.dsl.Http4sDsl


object TracingService extends Http4sDsl[IO] {
  val service: HttpRoutes[IO] = HttpService[IO] {
    case GET -> Root / "path" =>
      Ok()
  }


   // optional service data gets added as attribute to the created span
  val serviceData = ServiceData(name = "MyService", version = "1.2.3")
  val withTracingMiddleware: HttpRoutes[IO] =
    TracingMiddleware.withoutSpan(service, serviceData)
}

```

### Client

```scala
  import cats.effect.IO
  import org.http4s.client.Client
  import io.opencensus.scala.http4s.implicits._

  val client: Client[IO] = ???

  // trace request response
  val tracedClient: Client[IO] = client.traced
  // trace starting from another parentSpan
  val tracedClientWithParentSpan: Client[IO] = client.traced(parentSpan)

  // tracedClient is just a regular `Client[IO]`, so all of http4s methods are available
  val result: IO[String] =
    tracedClient.expect[String]("http://example.com/test")
```

The `traced` function enriches the `Client[F]`,
 it starts a new span, sets the HttpAttributes to the `Response[F]` and adds headers in the 
 [B3 Format](https://github.com/openzipkin/b3-propagation#overall-process) to the `Request`.

When the call completes or fails the span is ended with a proper status which fits to the http response code.

## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)
