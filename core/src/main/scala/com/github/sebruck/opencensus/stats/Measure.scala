package com.github.sebruck.opencensus.stats

import scala.util.Try

import io.opencensus.stats.{Measure => JavaMeasure}
import io.opencensus.stats.Measure.{
  MeasureDouble => JavaMeasureDouble,
  MeasureLong => JavaMeasureLong
}

sealed trait Measure[T] {
  type JavaM <: JavaMeasure
  def name: String
  def description: String
  def unit: String
  def javaMeasure: JavaM

  def fold[A](long: MeasureLong => A, double: MeasureDouble => A): A
}

object Measure {
  def long(name: String,
           description: String,
           unit: String): Try[Measure[Long]] = Try {
    new MeasureLong(name,
                    description,
                    unit,
                    JavaMeasureLong.create(name, description, unit)) {}
  }

  def double(name: String,
             description: String,
             unit: String): Try[Measure[Double]] = Try {
    new MeasureDouble(name,
                      description,
                      unit,
                      JavaMeasureDouble.create(name, description, unit)) {}
  }
}

sealed abstract case class Measurement[T](measure: Measure[T], value: T)
object Measurement {
  def long(measure: Measure[Long], value: Long): Measurement[Long] =
    new Measurement(measure, value) {}
  def double(measure: Measure[Double], value: Double): Measurement[Double] =
    new Measurement(measure, value) {}
}

sealed abstract case class MeasureLong(name: String,
                                       description: String,
                                       unit: String,
                                       javaMeasure: JavaMeasureLong)
    extends Measure[Long] {

  type JavaM = JavaMeasureLong
  override def fold[A](long: MeasureLong => A, double: MeasureDouble => A): A =
    long(this)
}
sealed abstract case class MeasureDouble(name: String,
                                         description: String,
                                         unit: String,
                                         javaMeasure: JavaMeasureDouble)
    extends Measure[Double] {
  type JavaM = JavaMeasureDouble
  override def fold[B](long: MeasureLong => B, double: MeasureDouble => B): B =
    double(this)
}
