import Dependencies._
import CompilerOptions._

name in ThisBuild := "opencensus-scala"
organization in ThisBuild := "com.github.sebruck"
scalaVersion in ThisBuild := "2.12.4"
scalacOptions in ThisBuild ++= compilerOptions
publishTo in ThisBuild := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

lazy val root = (project in file("."))
    .settings(publishArtifact := false)
    .aggregate(core, akkaHttp)

lazy val core = (project in file("core")).
  settings(
    name := "opencensus-scala-core",
    libraryDependencies := coreDependencies,
  )

lazy val akkaHttp = (project in file("akka-http")).
  settings(
    name := "opencensus-scala-akka-http",
    libraryDependencies := akkaHttpDependencies,
  ).dependsOn(core)
