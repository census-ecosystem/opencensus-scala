# opencensus-scala
This project is a lightweight scala wrapper for the 
[opencensus-java](https://github.com/census-instrumentation/opencensus-java) library 
and provides instrumentation for scala based frameworks, currently only Akka HTTP.

## Quickstart
### Dependencies
Add the following dependencies to your sbt project.

```scala
// TODO: Add version once the first version is published

// If you want to use only the core module
"com.github.sebruck" %% "opencensus-scala-core" % "" 

// If you want to use opencensus-scala inside an Akka HTTP project 
"com.github.sebruck" %% "opencensus-scala-akka-http" % "" 
```

### Configuration
opencensus-scala uses [typesafe config](https://github.com/lightbend/config) to configure exporters,
sampling rates and many more. For a full reference have a look at 
[the default configuration](core/src/main/resources/reference.conf).

To activate the Stackdriver trace exporter with the default sampling rate of 1/10000 add the following 
to your application.conf.
```
opencensus-scala {
    stackdriver {
        enabled = true
        project-id = "MY-GC-Project"
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
