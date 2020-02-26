package io.opencensus.scala.http.testSuite

import io.opencensus.scala.Stats
import io.opencensus.scala.stats.{Measurement, Tag, View}

import scala.util.{Success, Try}

class MockStats extends Stats {

  @volatile private var _recordedMeasurements =
    List.empty[(Measurement, List[Tag])]

  @volatile private var _registeredViews = List.empty[View]

  override def record(tags: List[Tag], measurements: Measurement*): Unit = {
    val measurementsWithTags = measurements.map(m => (m, tags))

    _recordedMeasurements = _recordedMeasurements ++ measurementsWithTags
  }

  override def record(measurements: Measurement*): Unit = {
    val measurementsWithTags = measurements.map(m => (m, List.empty[Tag]))

    _recordedMeasurements = _recordedMeasurements ++ measurementsWithTags
  }

  override def registerView(view: View): Try[Unit] = {
    _registeredViews = _registeredViews :+ view
    Success(())
  }

  def recordedMeasurements: Seq[(Measurement, List[Tag])] =
    _recordedMeasurements
  def registeredViews: Seq[View] = _registeredViews
}
