package com.github.sebruck.opencensus.trace.exporters

import com.github.sebruck.opencensus.StackdriverConfig
import com.google.auth.oauth2.GoogleCredentials
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.stackdriver.{
  StackdriverTraceConfiguration,
  StackdriverTraceExporter
}

import scala.collection.JavaConverters._

private[opencensus] object Stackdriver extends LazyLogging {

  def init(config: StackdriverConfig): Unit = {
    log(config)
    StackdriverTraceExporter.createAndRegister(buildConfig(config))
  }

  private def buildConfig(
      config: StackdriverConfig): StackdriverTraceConfiguration = {
    import config._

    val stackdriverConfig = StackdriverTraceConfiguration
      .builder()
      .setProjectId(projectId)

    credentialsFile.foreach { path =>
      val credentials = GoogleCredentials
        .fromStream(this.getClass.getResourceAsStream(path))
        .createScoped(
          Set("https://www.googleapis.com/auth/cloud-platform",
              "https://www.googleapis.com/auth/trace.append").asJava)

      stackdriverConfig.setCredentials(credentials)
    }

    stackdriverConfig.build()
  }

  private def log(config: StackdriverConfig): Unit = {
    import config._

    val credentialsLogInfo = credentialsFile
      .map(path => s"with credentials file $path")
      .getOrElse("")

    logger.info(
      s"Enabling StackdriverTraceExporter with project id $projectId" + credentialsLogInfo)
  }
}
