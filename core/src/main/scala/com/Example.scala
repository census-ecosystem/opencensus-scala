package com

import com.github.sebruck.opencensus.Stats
import com.github.sebruck.opencensus.stats._

object Example extends App {
  import java.io.{StringWriter, Writer}

  import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector
  import io.prometheus.client.Collector.MetricFamilySamples
  import io.prometheus.client.CollectorRegistry
  import io.prometheus.client.exporter.common.TextFormat

  PrometheusStatsCollector.createAndRegister()

  val measure      = Measure.long("measurename", "measure description", "munit").get
  val measureD     = Measure.double("doublemeasure", "description", "dunit").get
  val measurement  = Measurement.long(measure, 2L)
  val measurement2  = Measurement.long(measure, 3L)
  val measurementD = Measurement.double(measureD, 2.0)

  val view = View("myview",
                  "view description",
                  measure,
                  List("Col1", "Col2"),
                  Distribution(List(0.5, 1.0, 1.5, 2.0, 2.5, 3.0)).get).get

  val view2 =
    View("myview2", "view description2", measure, List("Col1", "col2"), Sum).get
  val viewD =
    View("viewd", "descr", measureD, List("foo", "bar"), LastValue).get

  Stats.registerView(view).get
  Stats.registerView(view2).get
  Stats.registerView(viewD).get

  val view2Tags = List(Tag("Col1", "test1").get,
                       Tag("Col2", "test2").get,
                       Tag("xxxx", "yyyy").get)
  Stats.record(view2Tags, measurement)

  Stats.record(view2Tags, measurement, measurement2)

  // Prometheus
  val samples = CollectorRegistry.defaultRegistry.metricFamilySamples()

  private def toPrometheusTextFormat(
      samples: java.util.Enumeration[MetricFamilySamples]): String = {
    val writer: Writer = new StringWriter()
    TextFormat.write004(writer, samples)
    writer.toString
  }
  Thread.sleep(5101)
  println(toPrometheusTextFormat(samples))
}
