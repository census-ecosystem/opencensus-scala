package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging
import io.opentelemetry.exporters.logging.LoggingSpanExporter
import io.opentelemetry.sdk.trace.export.SpanExporter

private[scala] object Logging extends LazyLogging {
  def init(): SpanExporter = {
    logger.info("Enabling LoggingTraceExporter")
    new LoggingSpanExporter()
  }
}
