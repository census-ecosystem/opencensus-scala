package io.opencensus.scala

import scala.concurrent.duration.FiniteDuration

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

private[scala] case class OcAgentTraceExporterConfig(
    enabled: Boolean,
    serviceName: String,
    endpoint: String,
    deadline: FiniteDuration,
    retryInterval: FiniteDuration,
    useInsecure: Boolean
)

private[scala] case class JaegerTraceExporterConfig(
    enabled: Boolean,
    serviceName: String,
    endpoint: String,
    deadline: FiniteDuration
)

private[scala] case class ElasticsearchTraceExporterConfig(
    enabled: Boolean,
    serviceName: String,
    url: String,
    index: String,
    `type`: String,
    username: Option[String],
    password: Option[String],
    deadline: FiniteDuration
)

private[scala] case class DatadogTraceExporterConfig(
    enabled: Boolean,
    serviceName: String,
    endpoint: String,
    `type`: String,
    deadline: FiniteDuration
)

private[scala] case class TraceExportersConfig(
    stackdriver: StackdriverTraceExporterConfig,
    logging: LoggingTraceExporterConfig,
    zipkin: ZipkinTraceExporterConfig,
    instana: InstanaTraceExporterConfig,
    ocagent: OcAgentTraceExporterConfig,
    jaeger: JaegerTraceExporterConfig,
    datadog: DatadogTraceExporterConfig,
    elasticsearch: ElasticsearchTraceExporterConfig
)

private[scala] case class TraceConfig(
    samplingProbability: Double,
    exporters: TraceExportersConfig
)

private[scala] case class Config(trace: TraceConfig)
