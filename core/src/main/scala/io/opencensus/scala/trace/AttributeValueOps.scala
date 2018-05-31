package io.opencensus.scala.trace

import io.opencensus.trace.AttributeValue

private[scala] object AttributeValueOps {

  implicit def toStringValue(s: String): AttributeValue =
    AttributeValue.stringAttributeValue(s)

  implicit def toLongValue(l: Long): AttributeValue =
    AttributeValue.longAttributeValue(l)
}
