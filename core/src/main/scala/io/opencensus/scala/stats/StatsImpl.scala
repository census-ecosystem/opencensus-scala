package io.opencensus.scala.stats

import io.opencensus.scala.Stats
import io.opencensus.stats.{MeasureMap, StatsRecorder, ViewManager}
import io.opencensus.tags.Tagger

import scala.util.Try

private[scala] class StatsImpl(
    viewManager: ViewManager,
    statsRecorder: StatsRecorder,
    tagger: Tagger
) extends Stats {

  override def record(measurements: Measurement*): Unit =
    record(List.empty, measurements: _*)

  override def record(tags: List[Tag], measurements: Measurement*): Unit = {
    val tagContext = tags
      .foldLeft(tagger.emptyBuilder())((builder, tag) =>
        builder.putPropagating(tag.key, tag.value)
      )
      .build()

    val measureMap =
      measurements.foldLeft(statsRecorder.newMeasureMap())(putMeasurement)

    measureMap.record(tagContext)
  }

  override def registerView(view: View): Try[Unit] = Try {
    viewManager.registerView(view.javaView)
  }

  private def putMeasurement(
      measureMap: MeasureMap,
      measurement: Measurement
  ): MeasureMap =
    measurement match {
      case MeasurementLong(m, value)   => measureMap.put(m.javaMeasure, value)
      case MeasurementDouble(m, value) => measureMap.put(m.javaMeasure, value)
    }
}
