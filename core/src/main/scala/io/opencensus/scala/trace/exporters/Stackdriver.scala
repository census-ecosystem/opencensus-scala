package io.opencensus.scala.trace.exporters

import com.google.auth.oauth2.GoogleCredentials
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.stackdriver.{
  StackdriverTraceConfiguration,
  StackdriverTraceExporter
}
import io.opencensus.scala.StackdriverTraceExporterConfig

import scala.collection.JavaConverters._

private[scala] object Stackdriver extends LazyLogging {

  def init(config: StackdriverTraceExporterConfig): Unit = {
    log(config)
    StackdriverTraceExporter.createAndRegister(buildConfig(config))
  }

  private def buildConfig(
      config: StackdriverTraceExporterConfig
  ): StackdriverTraceConfiguration = {
    import config._

    val stackdriverConfig = StackdriverTraceConfiguration
      .builder()
      .setProjectId(projectId)

    credentialsFile.foreach { path =>
      val credentials = GoogleCredentials
        .fromStream(this.getClass.getResourceAsStream(path))
        .createScoped(
          Set(
            "https://www.googleapis.com/auth/cloud-platform",
            "https://www.googleapis.com/auth/trace.append"
          ).asJava
        )

      stackdriverConfig.setCredentials(credentials)
    }

    stackdriverConfig.build()
  }

  private def log(config: StackdriverTraceExporterConfig): Unit = {
    import config._

    val credentialsLogInfo = credentialsFile
      .map(path => s"with credentials file $path")
      .getOrElse("")

    logger.info(
      s"Enabling StackdriverTraceExporter with project id $projectId" + credentialsLogInfo
    )
  }
}
