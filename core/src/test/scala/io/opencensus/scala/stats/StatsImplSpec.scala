package io.opencensus.scala.stats

import io.opencensus.implcore.internal.SimpleEventQueue
import io.opencensus.implcore.stats.StatsComponentImplBase
import io.opencensus.stats.AggregationData
import io.opencensus.stats.AggregationData.{SumDataDouble, SumDataLong}
import io.opencensus.tags.{Tags => JavaTags}
import io.opencensus.testing.common.TestClock
import org.scalatest.{FlatSpec, Inspectors, Matchers}

import scala.collection.JavaConverters._

class StatsImplSpec extends StatsSpecs {

  def measureLong(name: String) =
    Measure.long(name, "desc", "unit").get
  def measureDouble(name: String) =
    Measure.double(name, "desc", "unit").get

  val measurementsLong =
    List(Measurement.long(measureLong("name"), 4L)   -> SumDataLong.create(4L),
         Measurement.long(measureLong("name2"), 12L) -> SumDataLong.create(12L))

  val measurementsDouble = List(
    Measurement.double(measureDouble("name"), 4.0) -> SumDataDouble.create(4.0),
    Measurement.double(measureDouble("name2"), 12.0) -> SumDataDouble.create(
      12.0))

  "record single measure long" should behave like recordingSpecs(
    measurementsLong.take(1))

  "record single measure double" should behave like recordingSpecs(
    measurementsDouble.take(1))

  "record different long measures in batch" should behave like recordingSpecs(
    measurementsLong)

  "record different double measures in batch" should behave like recordingSpecs(
    measurementsDouble)
}

trait StatsSpecs extends FlatSpec with Matchers with Inspectors {

  def recordingSpecs(
      measurements: List[(Measurement, AggregationData)]
  ): Unit = {
    def view(measure: Measure, name: String) =
      View(name, "viewdesc", measure, List("col1"), Sum).get

    it should "record measurements" in {
      val (statsComponent, stats) = createStats()

      val views = measurements.zipWithIndex.map {
        case ((measurment, result), i) =>
          val testView = view(measurment.measure, i.toString)
          stats.registerView(testView)

          (testView, result)
      }

      stats.record(measurements.map(_._1): _*)

      forAll(views) {
        case (view, result) =>
          val jView =
            statsComponent.getViewManager.getView(view.javaView.getName)
          val values = jView.getAggregationMap.asScala.values
          values.head shouldBe result
      }
    }
  }

  private def createStats() = {
    val statsComponent =
      new StatsComponentImplBase(new SimpleEventQueue, TestClock.create())

    (statsComponent,
     new StatsImpl(statsComponent.getViewManager,
                   statsComponent.getStatsRecorder,
                   JavaTags.getTagger))

  }

}
