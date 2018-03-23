import sbt._

object Dependencies {

  val OpencensusVersion   = "0.12.2"
  val ScalaTestVersion    = "3.0.4"
  val PureConfigVersion   = "0.9.0"
  val ScalaLoggingVersion = "3.8.0"
  val AkkaHttpVersion     = "10.1.0"
  val AkkaVersion         = "2.5.11"

  lazy val opencensus = Seq(
    "opencensus-api",
    "opencensus-exporter-trace-stackdriver",
    "opencensus-impl"
  ).map(art => "io.opencensus" % art % OpencensusVersion)

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )

  lazy val config = Seq(
    "com.github.pureconfig" %% "pureconfig" % PureConfigVersion
  )

  lazy val logging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion
  )

  lazy val coreDependencies = opencensus ++ config ++ logging ++ scalaTest
  lazy val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-http"         % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream"       % AkkaVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test
  ) ++ scalaTest
}
