package com.github.sebruck.opencensus.stats

import scala.util.Try

import io.opencensus.stats.{Measure => JavaMeasure}
import io.opencensus.stats.Measure.{
  MeasureDouble => JavaMeasureDouble,
  MeasureLong => JavaMeasureLong
}

sealed trait Measure {
  type Value
  type JavaM <: JavaMeasure
  def name: String
  def description: String
  def unit: String
  def javaMeasure: JavaM
}

sealed abstract case class MeasureLong(name: String,
                                       description: String,
                                       unit: String,
                                       javaMeasure: JavaMeasureLong)
    extends Measure {
  type Value = Long
  type JavaM = JavaMeasureLong
}

sealed abstract case class MeasureDouble(name: String,
                                         description: String,
                                         unit: String,
                                         javaMeasure: JavaMeasureDouble)
    extends Measure {
  type Value = Double
  type JavaM = JavaMeasureDouble
}

object Measure {
  def long(name: String, description: String, unit: String): Try[MeasureLong] =
    Try {
      new MeasureLong(name,
                      description,
                      unit,
                      JavaMeasureLong.create(name, description, unit)) {}
    }

  def double(name: String, description: String, unit: String): Try[MeasureDouble] =
    Try {
      new MeasureDouble(name,
                        description,
                        unit,
                        JavaMeasureDouble.create(name, description, unit)) {}
    }
}

sealed trait Measurement {
  type Value = measure.Value
  val measure: Measure
  val value: Value
}
case class LongMeasurement(measure: MeasureLong, value: Long)
    extends Measurement
case class DoubleMeasurement(measure: MeasureDouble, value: Double)
    extends Measurement

object Measurement {
  def long(measure: MeasureLong, value: Long): Measurement =
    LongMeasurement(measure, value)
  def double(measure: MeasureDouble, value: Double): Measurement =
    DoubleMeasurement(measure, value)
}
