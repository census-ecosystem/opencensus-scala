package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.ocagent.OcAgentTraceExporter
import io.opencensus.exporter.trace.ocagent.OcAgentTraceExporterConfiguration
import io.opencensus.scala.OcAgentTraceExporterConfig
import io.opencensus.common.Duration

private[scala] object OcAgent extends LazyLogging {
  def init(config: OcAgentTraceExporterConfig): Unit = {
    logger.info(
      s"Enabling OcAgentTraceExporter with url ${config.endpoint} " +
        s"and service name ${config.serviceName}"
    )
    OcAgentTraceExporter.createAndRegister(
      OcAgentTraceExporterConfiguration
        .builder()
        .setEndPoint(config.endpoint)
        .setServiceName(config.serviceName)
        .setDeadline(Duration.fromMillis(config.deadline.toMillis))
        .setRetryInterval(Duration.fromMillis(config.retryInterval.toMillis))
        .setUseInsecure(config.useInsecure)
        .build()
    )
  }
}
