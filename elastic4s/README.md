# opencensus Elastic4s instrumentation
This modules contains utilities to use opencensus in Elastic4s applications.

The API documentation can be found [here](https://census-ecosystem.github.io/opencensus-scala/api/).

## Quickstart
In your build.sbt add the following dependency:

```scala
"com.github.sebruck" %% "opencensus-scala-elastic4s" % "0.6.0" 

// Dependent on the trace exporters you want to use add one or more of the following
"io.opencensus" % "opencensus-exporter-trace-stackdriver" % "0.15.0"
"io.opencensus" % "opencensus-exporter-trace-logging"     % "0.15.0"
"io.opencensus" % "opencensus-exporter-trace-instana"     % "0.15.0"
"io.opencensus" % "opencensus-exporter-trace-zipkin"      % "0.15.0"
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


The `TracingElasticClient` starts a new span either from the passed in parent span or without a parent span.

When the request to elasticserarch completes or fails the span is ended with a proper status which fits to the http response code.


### Client

```scala
import io.opencensus.scala.elastic4s.implicits._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticProperties
import io.opencensus.trace.BlankSpan

import scala.concurrent.ExecutionContext.Implicits.global

object ExampleElastic4s extends App {
  val elasticClient: ElasticClient = ElasticClient(ElasticProperties("elasticsearch://localhost:9200"))
  // without a parent span
  elasticClient.traced
    .execute(search("indexName"))
  //  Future[Either[RequestFailure, RequestSuccess[SearchResponse]]]
  // from a parent span
  val parentSpan = BlankSpan.INSTANCE
  elasticClient
    .traced(parentSpan)
    .execute(search("indexName"))
  //  Future[Either[RequestFailure, RequestSuccess[SearchResponse]]]
}
```

The `traced` function enriches the `ElasticClient`,
 it starts a new span and sets the HttpAttributes dependent on the `Request` and `RequestFailure` or `RequestSuccess`.

When the call completes or fails the span is ended with a proper status which fits to the http response code.

## Configuration
Have a look at the [default configuration](src/main/resources/reference.conf)
