import sbt._

object Dependencies {

  val OpencensusVersion   = "0.18.0"
  val ScalaTestVersion    = "3.0.5"
  val PureConfigVersion   = "0.10.1"
  val ScalaLoggingVersion = "3.9.2"
  val AkkaHttpVersion     = "10.1.7"
  val AkkaVersion         = "2.5.19"
  val Http4sVersion       = "0.18.21"
  val Elastic4sVersion    = "6.5.0"
  val CatsVersion         = "1.5.0"
  val DoobieVersion       = "0.6.0"

  lazy val opencensus = Seq(
    "io.opencensus" % "opencensus-api"                        % OpencensusVersion,
    "io.opencensus" % "opencensus-impl"                       % OpencensusVersion,
    "io.opencensus" % "opencensus-testing"                    % OpencensusVersion % Test,
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

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % CatsVersion
  )

  lazy val logging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion
  )

  lazy val coreDependencies = opencensus ++ pureConfig ++ cats ++ logging ++ scalaTest
  lazy val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-http"         % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream"       % AkkaVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-testkit"      % AkkaVersion % Test
  ) ++ scalaTest

  lazy val http4sDependencies = Seq(
    "org.http4s" %% "http4s-dsl"    % Http4sVersion,
    "org.http4s" %% "http4s-client" % Http4sVersion
  ) ++ scalaTest

  lazy val elastic4sDependencies = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion
  ) ++ scalaTest

  lazy val doobieDependencies = Seq(
    "org.tpolecat" %% "doobie-core" % DoobieVersion
  ) ++ scalaTest ++ cats
}
