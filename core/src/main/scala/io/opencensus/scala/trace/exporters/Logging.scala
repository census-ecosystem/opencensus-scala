package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.logging.LoggingTraceExporter
import io.opencensus.scala.LoggingTraceExporterConfig

private[scala] object Logging extends LazyLogging {

  def init(config: LoggingTraceExporterConfig): Unit = {
    logger.info("Enabling LoggingTraceExporter")
    LoggingTraceExporter.register()
  }
}
