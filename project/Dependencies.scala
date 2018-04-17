import sbt._

object Dependencies {

  val OpencensusVersion   = "0.12.3"
  val ScalaTestVersion    = "3.0.4"
  val PureConfigVersion   = "0.9.0"
  val ScalaLoggingVersion = "3.8.0"
  val AkkaHttpVersion     = "10.1.0"
  val AkkaVersion         = "2.5.11"

  lazy val opencensus = Seq(
    "io.opencensus" % "opencensus-api"                        % OpencensusVersion,
    "io.opencensus" % "opencensus-impl"                       % OpencensusVersion,
    "io.opencensus" % "opencensus-exporter-trace-stackdriver" % OpencensusVersion % Provided,
    "io.opencensus" % "opencensus-exporter-trace-logging"     % OpencensusVersion % Provided,
    "io.opencensus" % "opencensus-exporter-trace-instana"     % OpencensusVersion % Provided,
    "io.opencensus" % "opencensus-exporter-trace-zipkin"      % OpencensusVersion % Provided
  )

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )

  lazy val pureConfig = Seq(
    "com.github.pureconfig" %% "pureconfig" % PureConfigVersion
  )

  lazy val logging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion
  )

  lazy val coreDependencies = opencensus ++ pureConfig ++ logging ++ scalaTest
  lazy val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-http"         % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream"       % AkkaVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test
  ) ++ scalaTest
}
