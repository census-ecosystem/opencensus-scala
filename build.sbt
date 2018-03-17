import Dependencies._
import CompilerOptions._

name in ThisBuild := "opencensus-scala"
organization in ThisBuild := "com.github.sebruck"
scalaVersion in ThisBuild := "2.12.4"
version in ThisBuild := "0.1.0-SNAPSHOT"
scalacOptions in ThisBuild ++= compilerOptions

lazy val core = (project in file("core")).
  settings(
    name := "opencensus-scala-core",
    libraryDependencies := coreDependencies,
  )