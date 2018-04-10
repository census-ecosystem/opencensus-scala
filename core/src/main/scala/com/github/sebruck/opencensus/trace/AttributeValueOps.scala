package com.github.sebruck.opencensus.trace

import io.opencensus.trace.AttributeValue

private[opencensus] object AttributeValueOps {

  implicit def toStringValue(s: String): AttributeValue =
    AttributeValue.stringAttributeValue(s)

  implicit def toLongValue(l: Long): AttributeValue =
    AttributeValue.longAttributeValue(l)
}
