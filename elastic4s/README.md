# opencensus Elastic4s instrumentation
This modules contains utilities to use opencensus in Elastic4s applications.

The API documentation can be found [here](https://sebruck.github.io/opencensus-scala/).

## Quickstart
In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-elastic4s" % "0.4.3" 

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

### Tracing


The `TracingHttpClient` starts a new span either from the passed in parent span or without a parent span.

When the request to elasticserarch completes or fails the span is ended with a proper status which fits to the http response code.


### Client

```scala
import com.github.sebruck.opencensus.elastic4s.implicits._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import io.opencensus.trace.BlankSpan

import scala.concurrent.ExecutionContext.Implicits.global

object ExampleElastic4s extends App {
  val httpClient: HttpClient = HttpClient("elasticsearch://localhost:9200")

  // without a parent span
  httpClient.traced
    .execute(search("indexName"))
  //  Future[Either[RequestFailure, RequestSuccess[SearchResponse]]]

  // from a parent span
  val parentSpan = BlankSpan.INSTANCE
  httpClient
    .traced(parentSpan)
    .execute(search("indexName"))
  //  Future[Either[RequestFailure, RequestSuccess[SearchResponse]]]
}
```

The `traced` function enriches the `HttpClient`,
 it starts a new span and sets the HttpAttributes dependent on the `Request` and `RequestFailure` or `RequestSuccess`.

When the call completes or fails the span is ended with a proper status which fits to the http response code.

## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)
