package com.github.sebruck.opencensus

private[opencensus] case class StackdriverConfig(
    enabled: Boolean,
    projectId: String,
    credentialsFile: Option[String])

private[opencensus] case class Config(stackdriver: StackdriverConfig)
