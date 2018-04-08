package com.github.sebruck.opencensus

private[opencensus] case class StackdriverTraceExporterConfig(
    enabled: Boolean,
    projectId: String,
    credentialsFile: Option[String])

private[opencensus] case class LoggingTraceExporterConfig(
    enabled: Boolean
)

private[opencensus] case class TraceExportersConfig(
    stackdriver: StackdriverTraceExporterConfig,
    logging: LoggingTraceExporterConfig)

private[opencensus] case class TraceConfig(samplingProbability: Double,
                                           exporters: TraceExportersConfig)

private[opencensus] case class Config(trace: TraceConfig)
