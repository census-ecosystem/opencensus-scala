package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging
import io.opentelemetry.exporters.zipkin.{
  ZipkinExporterConfiguration,
  ZipkinSpanExporter
}
import io.opencensus.scala.ZipkinTraceExporterConfig

private[scala] object Zipkin extends LazyLogging {

  def init(config: ZipkinTraceExporterConfig): ZipkinSpanExporter = {
    logger.info(
      s"Enabling LoggingTraceExporter with url ${config.v2Url} " +
        s"and service name ${config.serviceName}"
    )
    ZipkinSpanExporter.create(
      ZipkinExporterConfiguration
        .builder()
        .setEndpoint(config.v2Url)
        .setServiceName(config.serviceName)
        .build()
    )
  }
}
