# opencensus Akka HTTP instrumentation
This modules contains utilities to use opencensus in Akka HTTP applications.

The API documentation can be found [here](https://sebruck.github.io/opencensus-scala/).

## Quickstart
In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-akka-http" % "0.4.1" 

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

### Server
```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

object TracingService extends App {
  import com.github.sebruck.opencensus.akka.http.TracingDirective._

  implicit val system: ActorSystem             = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
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

## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)
