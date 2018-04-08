[![Build Status](https://travis-ci.org/Sebruck/opencensus-scala.svg?branch=master)](https://travis-ci.org/Sebruck/opencensus-scala)
# opencensus-scala
This project is a lightweight scala wrapper for the 
[opencensus-java](https://github.com/census-instrumentation/opencensus-java) library 
and provides instrumentation for scala based frameworks.

## Implementation status

### Tracing
|Exporter    |Status   |
|------------|---------|
|Instana     |planned  |
|Logging     |supported|
|Stackdriver |supported|
|Zipkin      |planned  |

### Stats
Not supported yet.

### Instrumentations
|Framework|Tracing  |Stats    |
|---------|---------|---------|
|Akka HTTP|supported|planned  |
|Play     |planned  |planned  |
|Http4s   |planned  |planned  |


## Quickstart
### Dependencies
Add the following dependencies to your sbt project.

```scala
// If you want to use only the core module
"com.github.sebruck" %% "opencensus-scala-core" % "0.1.2" 

// Dependent on the trace exporters you want to use add one or more of the following
"io.opencensus" % "opencensus-exporter-trace-stackdriver" % "0.12.2"
"io.opencensus" % "opencensus-exporter-trace-logging"     % "0.12.2"
"io.opencensus" % "opencensus-exporter-trace-instana"     % "0.12.2"
"io.opencensus" % "opencensus-exporter-trace-zipkin"      % "0.12.2"

// If you want to use opencensus-scala inside an Akka HTTP project 
"com.github.sebruck" %% "opencensus-scala-akka-http" % "0.1.2" 
```

### Configuration
opencensus-scala uses [typesafe config](https://github.com/lightbend/config) to configure exporters,
sampling rates and many more. For a full reference have a look at 
[the default configuration](core/src/main/resources/reference.conf).

To activate the Stackdriver trace exporter with the default sampling rate of 1/10000 add the following 
to your application.conf.
```
opencensus-scala {
  trace {
    exporters {
      stackdriver {
        enabled = true 
        project-id = "MY-GC-Project"
      }
    }
  }
}
```

For documentation how to use the several modules, have a look at the [modules](#modules) section.

## Modules

### Core
Utilities to use opencensus in a scala idiomatic way.

Learn more at the [documentation](core/README.md).

### Akka HTTP
Utilities to use opencensus in Akka HTTP applications. 

Learn more at the [documentation](akka-http/README.md).

## Contributing
Contributions are very welcome! As a starting point, have a look at the open issues. 
