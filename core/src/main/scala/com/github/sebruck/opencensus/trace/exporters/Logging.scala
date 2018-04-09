package com.github.sebruck.opencensus.trace.exporters

import com.github.sebruck.opencensus.LoggingTraceExporterConfig
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.logging.LoggingTraceExporter

private[opencensus] object Logging extends LazyLogging {

  def init(config: LoggingTraceExporterConfig): Unit =
    if (config.enabled) {
      logger.info("Enabling LoggingTraceExporter")
      LoggingTraceExporter.register()
    }
}
