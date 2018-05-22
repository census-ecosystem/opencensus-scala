package com.github.sebruck.opencensus.stats

import io.opencensus.tags.{TagKey, TagValue}
import cats.syntax.traverse._
import cats.instances.list._
import cats.instances.try_._

import scala.util.Try

object Tag {
  def apply(key: String, value: String): Try[Tag] =
    Try(new Tag(TagKey.create(key), TagValue.create(value)) {})

  def apply(keyValue: (String, String),
            keyValues: (String, String)*): Try[List[Tag]] =
    (keyValue :: keyValues.toList).traverse {
      case (key, value) => apply(key, value)
    }
}

sealed abstract case class Tag(key: TagKey, value: TagValue)
