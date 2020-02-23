package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.jaeger.JaegerTraceExporter
import io.opencensus.exporter.trace.jaeger.JaegerExporterConfiguration
import io.opencensus.scala.JaegerTraceExporterConfig
import io.opencensus.common.Duration

private[scala] object Jaeger extends LazyLogging {
  def init(config: JaegerTraceExporterConfig): Unit = {
    logger.info(
      s"Enabling JaegerTraceExporter with url ${config.endpoint} " +
        s"and service name ${config.serviceName}"
    )
    JaegerTraceExporter.createAndRegister(
      JaegerExporterConfiguration
        .builder()
        .setThriftEndpoint(config.endpoint)
        .setServiceName(config.serviceName)
        .setDeadline(Duration.fromMillis(config.deadline.toMillis))
        .build()
    )
  }
}
