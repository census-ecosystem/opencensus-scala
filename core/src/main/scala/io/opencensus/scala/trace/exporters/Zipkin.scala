package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.zipkin.{
  ZipkinExporterConfiguration,
  ZipkinTraceExporter
}
import io.opencensus.scala.ZipkinTraceExporterConfig

private[scala] object Zipkin extends LazyLogging {

  def init(config: ZipkinTraceExporterConfig): Unit = {
    logger.info(
      s"Enabling LoggingTraceExporter with url ${config.v2Url} " +
        s"and service name ${config.serviceName}"
    )
    ZipkinTraceExporter.createAndRegister(
      ZipkinExporterConfiguration
        .builder()
        .setV2Url(config.v2Url)
        .setServiceName(config.serviceName)
        .build()
    )
  }
}
