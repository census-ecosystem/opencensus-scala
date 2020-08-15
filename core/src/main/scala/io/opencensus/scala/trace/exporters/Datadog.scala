package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.datadog.DatadogTraceExporter
import io.opencensus.exporter.trace.datadog.DatadogTraceConfiguration
import io.opencensus.scala.DatadogTraceExporterConfig
import io.opencensus.common.Duration

private[scala] object Datadog extends LazyLogging {
  def init(config: DatadogTraceExporterConfig): Unit = {
    logger.info(
      s"Enabling DatadogTraceExporter with url ${config.endpoint} " +
        s"and service name ${config.serviceName}"
    )
    DatadogTraceExporter.createAndRegister(
      DatadogTraceConfiguration
        .builder()
        .setAgentEndpoint(config.endpoint)
        .setType(config.`type`)
        .setService(config.serviceName)
        .setDeadline(Duration.fromMillis(config.deadline.toMillis))
        .build()
    )
  }
}
