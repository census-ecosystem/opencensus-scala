import Dependencies._
import CompilerOptions._

name in ThisBuild := "opencensus-scala"
organization in ThisBuild := "com.github.sebruck"
scalaVersion in ThisBuild := "2.12.4"
scalacOptions in ThisBuild ++= compilerOptions
releasePublishArtifactsAction in ThisBuild := PgpKeys.publishSigned.value
publishTo in ThisBuild := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

lazy val root = (project in file("."))
    .settings(
      publishArtifact := false,
      siteSubdirName in ScalaUnidoc := "api",
      addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    )
  .enablePlugins(ScalaUnidocPlugin)
  .enablePlugins(SiteScaladocPlugin)
  .aggregate(core, akkaHttp, http4s, http)

lazy val core = (project in file("core")).
  settings(
    name := "opencensus-scala-core",
    libraryDependencies := coreDependencies,
  )

lazy val akkaHttp = (project in file("akka-http")).
  settings(
    name := "opencensus-scala-akka-http",
    libraryDependencies := akkaHttpDependencies,
  ).dependsOn(core, http % "compile->compile;test->test")

lazy val http4s = (project in file("http4s")).
  settings(
    name := "opencensus-scala-http4s",
    libraryDependencies := http4sDependencies,
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
  ).dependsOn(core, http % "compile->compile;test->test")

lazy val http = (project in file("http")).
  settings(
    name := "opencensus-scala-http",
    libraryDependencies := scalaTest,
  ).dependsOn(core)
