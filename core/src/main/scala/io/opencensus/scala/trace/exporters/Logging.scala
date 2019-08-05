package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.logging.LoggingTraceExporter

private[scala] object Logging extends LazyLogging {

  def init(): Unit = {
    logger.info("Enabling LoggingTraceExporter")
    LoggingTraceExporter.register()
  }
}
