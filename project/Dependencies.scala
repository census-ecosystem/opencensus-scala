import sbt._

object Dependencies {

  val OpencensusVersion   = "0.12.2"
  val ScalaTestVersion    = "3.0.4"
  val PureConfigVersion   = "0.9.0"
  val ScalaLoggingVersion = "3.8.0"

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
}
