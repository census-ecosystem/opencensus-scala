# opencensus Akka HTTP instrumentation
This modules contains utilities to use opencensus in Akka HTTP applications.

## Quickstart
In your build.sbt add the following dependency:

```scala
// TODO: Add version once the first version is published
"com.github.sebruck" %% "opencensus-scala-akka-http" % "" 
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

When the request completes or fails the span is ended with a proper status which fit****s to the http response code.


## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)
