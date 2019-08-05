import Dependencies._

name in ThisBuild := "opencensus-scala"
organization in ThisBuild := "com.github.sebruck"
scalaVersion in ThisBuild := "2.13.0"
scalafmtOnCompile in ThisBuild := true

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
    addMappingsToSiteDir(
      mappings in (ScalaUnidoc, packageDoc),
      siteSubdirName in ScalaUnidoc
    ),
    // This is needed for unidoc, otherwise it can not generate the scala doc
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
  )
  .enablePlugins(ScalaUnidocPlugin)
  .enablePlugins(SiteScaladocPlugin)
  .aggregate(core, akkaHttp, http4s, elastic4s, doobie, http)

lazy val core = (project in file("core")).settings(
  name := "opencensus-scala-core",
  libraryDependencies := coreDependencies
)

lazy val akkaHttp = (project in file("akka-http"))
  .settings(
    name := "opencensus-scala-akka-http",
    libraryDependencies := akkaHttpDependencies
  )
  .dependsOn(core, http % "compile->compile;test->test")

lazy val akkaHttpExample = (project in file("akka-http-example"))
  .settings(
    publishArtifact := false,
    libraryDependencies ++= Seq(
      "io.opencensus"  % "opencensus-exporter-trace-logging" % "0.23.0",
      "ch.qos.logback" % "logback-classic"                   % "1.2.3",
      "org.slf4j"      % "log4j-over-slf4j"                  % "1.7.25",
      "org.slf4j"      % "jul-to-slf4j"                      % "1.7.25"
    )
  )
  .dependsOn(akkaHttp)

lazy val http4s = (project in file("http4s"))
  .settings(
    name := "opencensus-scala-http4s",
    libraryDependencies := http4sDependencies,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
  )
  .dependsOn(core, http % "compile->compile;test->test")

lazy val elastic4s = (project in file("elastic4s"))
  .settings(
    name := "opencensus-scala-elastic4s",
    libraryDependencies := elastic4sDependencies
  )
  .dependsOn(core, http % "compile->compile;test->test")

lazy val doobie = (project in file("doobie"))
  .settings(
    name := "opencensus-scala-doobie",
    libraryDependencies := doobieDependencies,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
  )
  .dependsOn(core, http % "compile->compile;test->test")

lazy val http = (project in file("http"))
  .settings(
    name := "opencensus-scala-http",
    libraryDependencies := scalaTest
  )
  .dependsOn(core)
