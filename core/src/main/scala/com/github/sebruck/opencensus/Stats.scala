package com.github.sebruck.opencensus

import com.github.sebruck.opencensus.stats._
import io.opencensus.stats.{Stats => JavaStats}
import io.opencensus.tags.{Tags => JavaTags}

import scala.util.Try

trait Stats {
  def record(tags: List[Tag], measurements: Measurement[_]*): Unit
  def record(measurements: Measurement[_]*): Unit
  def registerView(view: View): Try[Unit]
}

object Stats
    extends StatsImpl(JavaStats.getViewManager,
                      JavaStats.getStatsRecorder,
                      JavaTags.getTagger)
