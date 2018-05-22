package com.github.sebruck.opencensus.stats

import io.opencensus.stats.{
  BucketBoundaries,
  Aggregation => JavaAggregation,
  View => JavaView
}
import io.opencensus.tags.TagKey

import scala.collection.JavaConverters._
import scala.util.Try

sealed trait Aggregation {
  def javaAggregation: JavaAggregation

  def fold[T](count: => T,
              sum: => T,
              lastValue: => T,
              distribution: Distribution => T): T
}

case object Count extends Aggregation {
  override def fold[T](count: => T,
                       sum: => T,
                       lastValue: => T,
                       distribution: Distribution => T): T = count

  override val javaAggregation: JavaAggregation = JavaAggregation.Count.create()
}

case object Sum extends Aggregation {
  override def fold[T](count: => T,
                       sum: => T,
                       lastValue: => T,
                       distribution: Distribution => T): T = sum

  override val javaAggregation: JavaAggregation = JavaAggregation.Sum.create()
}

case object LastValue extends Aggregation {
  override def fold[T](count: => T,
                       sum: => T,
                       lastValue: => T,
                       distribution: Distribution => T): T = lastValue

  override val javaAggregation: JavaAggregation =
    JavaAggregation.LastValue.create()
}

sealed abstract case class Distribution(
    buckets: List[Double],
    javaAggregation: JavaAggregation.Distribution)
    extends Aggregation {
  override def fold[T](count: => T,
                       sum: => T,
                       lastValue: => T,
                       distribution: Distribution => T): T =
    distribution(this)
}

object Distribution {
  def apply(buckets: List[Double]): Try[Distribution] = Try {

    val javaDistribution = JavaAggregation.Distribution.create(
      BucketBoundaries.create(buckets.map(new java.lang.Double(_)).asJava))

    new Distribution(buckets, javaDistribution) {}
  }
}

sealed abstract case class View(name: String,
                                description: String,
                                measure: Measure[_],
                                columns: List[String],
                                aggregation: Aggregation,
                                javaView: JavaView)

object View {
  def apply(name: String,
            description: String,
            measure: Measure[_],
            columns: List[String],
            aggregation: Aggregation): Try[View] = Try {

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
