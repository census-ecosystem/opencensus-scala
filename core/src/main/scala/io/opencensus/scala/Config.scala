package io.opencensus.scala

private[scala] case class StackdriverTraceExporterConfig(
    enabled: Boolean,
    projectId: String,
    credentialsFile: Option[String]
)

private[scala] case class LoggingTraceExporterConfig(
    enabled: Boolean
)

private[scala] case class ZipkinTraceExporterConfig(
    enabled: Boolean,
    v2Url: String,
    serviceName: String
)

private[scala] case class InstanaTraceExporterConfig(
    enabled: Boolean,
    agentEndpoint: String
)

private[scala] case class TraceExportersConfig(
    stackdriver: StackdriverTraceExporterConfig,
    logging: LoggingTraceExporterConfig,
    zipkin: ZipkinTraceExporterConfig,
    instana: InstanaTraceExporterConfig
)

private[scala] case class TraceConfig(
    samplingProbability: Double,
    exporters: TraceExportersConfig
)

private[scala] case class Config(trace: TraceConfig)
