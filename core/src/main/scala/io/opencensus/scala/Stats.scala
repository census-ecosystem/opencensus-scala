package io.opencensus.scala

import io.opencensus.scala.stats._
import io.opencensus.stats.{Stats => JavaStats}
import io.opencensus.tags.{Tags => JavaTags}

import scala.util.Try

/**
  * Api to record stats
  */
trait Stats {

  /**
    * Record multiple measurements with a specified set of tags.
    *
    * @param tags The tags which should be attached to the measurements.
    * @param measurements The measurements which should be recorded.
    */
  def record(tags: List[Tag], measurements: Measurement*): Unit

  /**
    * Records multiple measurements.
    *
    * @param measurements The measurements which should be recorded.
    */
  def record(measurements: Measurement*): Unit

  /**
    * Tries to register a view.
    *
    * @param view The view which should be registered.
    */
  def registerView(view: View): Try[Unit]
}

object Stats
    extends StatsImpl(JavaStats.getViewManager,
                      JavaStats.getStatsRecorder,
                      JavaTags.getTagger)
