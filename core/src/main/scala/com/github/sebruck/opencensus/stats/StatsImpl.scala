package com.github.sebruck.opencensus.stats

import com.github.sebruck.opencensus.Stats
import io.opencensus.stats.{MeasureMap, StatsRecorder, ViewManager}
import io.opencensus.tags.Tagger

import scala.util.Try

class StatsImpl(viewManager: ViewManager,
                statsRecorder: StatsRecorder,
                tagger: Tagger)
    extends Stats {

  override def record(measurements: Measurement[_]*): Unit =
    record(List.empty, measurements: _*)

  override def record(tags: List[Tag], measurements: Measurement[_]*): Unit = {
    val tagContext = tags
      .foldLeft(tagger.emptyBuilder())((builder, tag) =>
        builder.put(tag.key, tag.value))
      .build()

    val measureMap =
      measurements.foldLeft(statsRecorder.newMeasureMap())(putMeasurement)

    measureMap.record(tagContext)
  }

  override def registerView(view: View): Try[Unit] = Try {
    viewManager.registerView(view.javaView)
  }

  private def putMeasurement(measureMap: MeasureMap,
                             measurement: Measurement[_]): MeasureMap = {

    measurement.measure.fold(
      measureLong =>
        measurement.value match {
          case long: Long => measureMap.put(measureLong.javaMeasure, long)
          case _ =>
            throw new Exception("A long measure with a double value is invalid")
      },
      measureDouble =>
        measurement.value match {
          case double: Double =>
            measureMap.put(measureDouble.javaMeasure, double)
          case _ =>
            throw new Exception("A double measure with a long value is invalid")
      }
    )
  }
}
