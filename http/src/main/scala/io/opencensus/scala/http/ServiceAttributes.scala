package io.opencensus.scala.http

import io.opencensus.scala.trace.AttributeValueOps._
import io.opencensus.trace.Span

object ServiceAttributes {
  def setAttributesForService(span: Span, serviceData: ServiceData): Unit = {
    serviceData.name.foreach(span.putAttribute("service.name", _))
    serviceData.version.foreach(span.putAttribute("service.version", _))
  }
}
