package com.github.sebruck.opencensus.trace.exporters

import com.github.sebruck.opencensus.ZipkinTraceExporterConfig
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.zipkin.ZipkinTraceExporter

private[opencensus] object Zipkin extends LazyLogging {

  def init(config: ZipkinTraceExporterConfig): Unit = {
    logger.info(
      s"Enabling LoggingTraceExporter with url ${config.v2Url} " +
        s"and service name ${config.serviceName}")
    ZipkinTraceExporter.createAndRegister(config.v2Url, config.serviceName)
  }
}
