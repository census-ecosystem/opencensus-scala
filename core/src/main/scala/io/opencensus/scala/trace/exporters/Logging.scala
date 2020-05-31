package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging

private[scala] object Logging extends LazyLogging {
  def init(): Unit = logger.info("Enabling LoggingTraceExporter")
}
