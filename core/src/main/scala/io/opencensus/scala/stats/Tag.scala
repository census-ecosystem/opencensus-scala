package io.opencensus.scala.stats
import cats.syntax.traverse._
import io.opencensus.tags.{TagKey, TagValue}

import scala.util.Try

object Tag {

  /**
    * Creates a Tag
    *
    * @param key must not be longer than 255 characters and may contain only
    *            printable characters.
    * @param value must not be longer than 255 characters and may contain only
    *            printable characters.
    */
  def apply(key: String, value: String): Try[Tag] =
    Try(new Tag(TagKey.create(key), TagValue.create(value)) {})

  /**
    * Creates multiple tags with key value pairs.
    *
    * The first value of the tuple is the name and the second value is the value.
    * Both must not be longer than 255 characters and may contain only printable characters.
    */
  def apply(
      keyValue: (String, String),
      keyValues: (String, String)*
  ): Try[List[Tag]] =
    (keyValue :: keyValues.toList).traverse {
      case (key, value) => apply(key, value)
    }
}

/**
  * A Tag is a key value pair which can be added to a measurement.
  * @param key the key if of the tag
  * @param value the value of the tag
  */
sealed abstract case class Tag(key: TagKey, value: TagValue)
