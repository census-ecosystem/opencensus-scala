package io.opencensus.scala.stats

import io.opencensus.stats.Measure.{
  MeasureDouble => JavaMeasureDouble,
  MeasureLong => JavaMeasureLong
}
import io.opencensus.stats.{Measure => JavaMeasure}

import scala.util.Try

/**
  * A measure is something which can be measured in a measurement with a value.
  */
sealed trait Measure {

  /**
    * The type of the value
    */
  type Value

  /**
    * The type of the underlying java measure
    */
  type JavaM <: JavaMeasure

  /**
    * The measures name. Should be a ASCII string with a length no greater than
    * 255 characters.
    */
  def name: String

  /**
    * A description of the measure which is used in documentation.
    */
  def description: String

  /**
    * The units in which Measure values are measured.
    *
    * <p>The suggested grammar for a unit is as follows:
    *
    * <ul>
    *   <li>Expression = Component { "." Component } {"/" Component };
    *   <li>Component = [ PREFIX ] UNIT [ Annotation ] | Annotation | "1";
    *   <li>Annotation = "{" NAME "}" ;
    * </ul>
    *
    * <p>For example, string “MBy{transmitted}/ms” stands for megabytes per milliseconds, and the
    * annotation transmitted inside {} is just a comment of the unit.
    */
  def unit: String

  /**
    * The underlying measure of the java library.
    */
  def javaMeasure: JavaM
}

/** @inheritdoc */
sealed abstract case class MeasureLong(
    name: String,
    description: String,
    unit: String,
    javaMeasure: JavaMeasureLong
) extends Measure {
  type Value = Long
  type JavaM = JavaMeasureLong
}

/** @inheritdoc */
sealed abstract case class MeasureDouble(
    name: String,
    description: String,
    unit: String,
    javaMeasure: JavaMeasureDouble
) extends Measure {
  type Value = Double
  type JavaM = JavaMeasureDouble
}

object Measure {

  /**
    * Tries to creates a measure for long values.
    *
    * Can fail when passing an invalid name, description or unit.
    *
    * @param name The measures name. Should be a ASCII string with a length no greater than
    *             255 characters.
    * @param description A description of the measure which is used in documentation.
    * @param unit The units in which Measure values are measured.
    */
  def long(name: String, description: String, unit: String): Try[MeasureLong] =
    Try {
      new MeasureLong(
        name,
        description,
        unit,
        JavaMeasureLong.create(name, description, unit)
      ) {}
    }

  /**
    * Tries to creates a measure for double values.
    *
    * Can fail when passing an invalid name, description or unit.
    *
    * @param name The measures name. Should be a ASCII string with a length no greater than
    *             255 characters.
    * @param description A description of the measure which is used in documentation.
    * @param unit The units in which Measure values are measured.
    */
  def double(
      name: String,
      description: String,
      unit: String
  ): Try[MeasureDouble] =
    Try {
      new MeasureDouble(
        name,
        description,
        unit,
        JavaMeasureDouble.create(name, description, unit)
      ) {}
    }
}

/**
  * A Measurement records a value for the measure.
  */
sealed trait Measurement {

  /**
    * The type of the value, depends on the measure.
    */
  type Value = measure.Value

  /**
    * The measure for which a value should be recorded.
    */
  val measure: Measure

  /**
    * The value which should be recorded
    */
  val value: Value
}
private[stats] case class MeasurementLong(measure: MeasureLong, value: Long)
    extends Measurement
private[stats] case class MeasurementDouble(
    measure: MeasureDouble,
    value: Double
) extends Measurement

object Measurement {

  /**
    * Creates a measurement for a measure of type long, which can be recorded.
    * @param measure The measure for which a value should be recorded.
    * @param value The value which should be recorded
    */
  def long(measure: MeasureLong, value: Long): Measurement =
    MeasurementLong(measure, value)

  /**
    * Creates a measurement for a measure of type double, which can be recorded.
    * @param measure The measure for which a value should be recorded.
    * @param value The value which should be recorded
    */
  def double(measure: MeasureDouble, value: Double): Measurement =
    MeasurementDouble(measure, value)
}
