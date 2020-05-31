package io.opencensus.scala

private[scala] case class LoggingTraceExporterConfig(
    enabled: Boolean
)

private[scala] case class ZipkinTraceExporterConfig(
    enabled: Boolean,
    v2Url: String,
    serviceName: String
)

private[scala] case class TraceExportersConfig(
    logging: LoggingTraceExporterConfig,
    zipkin: ZipkinTraceExporterConfig
)

private[scala] case class TraceConfig(
    samplingProbability: Double,
    exporters: TraceExportersConfig
)

private[scala] case class Config(trace: TraceConfig)
