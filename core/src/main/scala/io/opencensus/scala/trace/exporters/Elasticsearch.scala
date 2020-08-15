package io.opencensus.scala.trace.exporters

import com.typesafe.scalalogging.LazyLogging
import io.opencensus.exporter.trace.elasticsearch.ElasticsearchTraceExporter
import io.opencensus.exporter.trace.elasticsearch.ElasticsearchTraceConfiguration
import io.opencensus.scala.ElasticsearchTraceExporterConfig
import io.opencensus.common.Duration

private[scala] object ElasticSearch extends LazyLogging {
  def init(config: ElasticsearchTraceExporterConfig): Unit = {
    logger.info(
      s"Enabling ElasticsearchTraceExporter with url ${config.url} " +
        s"and service name ${config.serviceName}"
    )
    ElasticsearchTraceExporter.createAndRegister(
      ElasticsearchTraceConfiguration
        .builder()
        .setElasticsearchIndex(config.index)
        .setElasticsearchUrl(config.url)
        .setUserName(config.username.orNull)
        .setPassword(config.password.orNull)
        .setAppName(config.serviceName)
        .setElasticsearchType(config.`type`)
        .setDeadline(Duration.fromMillis(config.deadline.toMillis))
        .build()
    )
  }
}
