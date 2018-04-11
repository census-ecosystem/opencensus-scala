package com.github.sebruck.opencensus.trace.exporters

import com.github.sebruck.opencensus.InstanaTraceExporterConfig
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.instana.InstanaTraceExporter

private[opencensus] object Instana extends LazyLogging {

  def init(config: InstanaTraceExporterConfig): Unit =
    if (config.enabled) {
      logger.info(
        s"Enabling InstanaTraceExporter with agent endpoint ${config.agentEndpoint}")
      InstanaTraceExporter.createAndRegister(config.agentEndpoint)
    }
}
