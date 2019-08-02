package io.opencensus.scala.stats

import io.opencensus.stats.{
  BucketBoundaries,
  Aggregation => JavaAggregation,
  View => JavaView
}
import io.opencensus.tags.TagKey

import scala.jdk.CollectionConverters._
import scala.util.Try

/**
  * Aggregation is the process of combining a certain set of Measurements for a
  * given Measure.
  *
  * <p>Aggregation currently supports 4 types of basic aggregation:
  *
  * <ul>
  *   <li>Sum
  *   <li>Count
  *   <li>Distribution
  *   <li>LastValue
  * </ul>
  *
  * <p>When creating a View, one Aggregation needs to be specified as how to
  * aggregate Measurements.
  *
  */
sealed trait Aggregation {

  /**
    * The underlying Aggregation of the java library.
    */
  def javaAggregation: JavaAggregation
}

/**
  * Calculate count on aggregated Measurements.
  */
case object Count extends Aggregation {

  /** @inheritdoc */
  override val javaAggregation: JavaAggregation = JavaAggregation.Count.create()
}

/**
  * Calculate sum on aggregated Measurements.
  */
case object Sum extends Aggregation {

  /** @inheritdoc */
  override val javaAggregation: JavaAggregation = JavaAggregation.Sum.create()
}

/**
  * Calculate the last value on aggregated Measurements.
  */
case object LastValue extends Aggregation {

  /** @inheritdoc */
  override val javaAggregation: JavaAggregation =
    JavaAggregation.LastValue.create()
}

/**
  * Calculate distribution stats on aggregated Measurements. Distribution includes mean,
  * count, histogram, min, max and sum of squared deviations.
  *
  * @param buckets the boundaries for the buckets in the underlying histogram.
  * @param javaAggregation The underlying Aggregation of the java library.
  */
sealed abstract case class Distribution(
    buckets: List[Double],
    javaAggregation: JavaAggregation.Distribution
) extends Aggregation {}

object Distribution {
  def apply(buckets: List[Double]): Try[Distribution] = Try {

    val javaDistribution = JavaAggregation.Distribution.create(
      BucketBoundaries.create(buckets.map(java.lang.Double.valueOf).asJava)
    )

    new Distribution(buckets, javaDistribution) {}
  }
}

/**
  * A View specifies an aggregation and a set of tag keys.
  * The aggregation will be broken down by the unique set of Measurement values for each measure.
  *
  * @param name Must be unique.
  * @param description Detailed description of the view for documentation purpose.
  * @param measure The measure type of this view
  * @param columns Columns (a.k.a Tag Keys) to match with the associated Measure.
  * @param aggregation The Aggregation associated with this View.
  * @param javaView The underlying View of the java library
  */
sealed abstract case class View(
    name: String,
    description: String,
    measure: Measure,
    columns: List[String],
    aggregation: Aggregation,
    javaView: JavaView
)

object View {

  /**
    * Tries to creates a View.
    *
    * Can fail when one of the parameters is contains an invalid value.
    *
    * @param name Must be unique.
    * @param description Detailed description of the view for documentation purpose.
    * @param measure The measure type of this view
    * @param columns Columns (a.k.a Tag Keys) to match with the associated Measure.
    * @param aggregation The Aggregation associated with this View.
    * @return
    */
  def apply(
      name: String,
      description: String,
      measure: Measure,
      columns: List[String],
      aggregation: Aggregation
  ): Try[View] = Try {

    val javaView = JavaView.create(
      JavaView.Name.create(name),
      description,
      measure.javaMeasure,
      aggregation.javaAggregation,
      columns.map(TagKey.create).asJava
    )

    new View(name, description, measure, columns, aggregation, javaView) {}
  }
}
