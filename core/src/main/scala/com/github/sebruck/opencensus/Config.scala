package com.github.sebruck.opencensus

private[opencensus] case class StackdriverConfig(
    enabled: Boolean,
    projectId: String,
    credentialsFile: Option[String])

private[opencensus] case class TraceExportersConfig(
    stackdriver: StackdriverConfig)
private[opencensus] case class TraceConfig(samplingProbability: Double,
                                           exporters: TraceExportersConfig)
private[opencensus] case class Config(trace: TraceConfig)
