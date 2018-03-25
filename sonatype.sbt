// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName in ThisBuild := "com.github.sebruck"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle in ThisBuild := true

publishArtifact in Test := false

// License of your choice
licenses in ThisBuild := Seq(
  "MIT" -> url("http://www.opensource.org/licenses/mit-license.php"))
homepage in ThisBuild := Some(url("https://github.com/Sebruck/opencensus-scala/"))
scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/Sebruck/opencensus-scala/"),
    "scm:git@github.com:sebruck/opencensus-scala.git"
  )
)
developers in ThisBuild := List(
  Developer(id = "sebruck",
            name = "Sebastian Bruckner",
            email = "sebbruck@googlemail.com",
            url = url("https://github.com/Sebruck"))
)
