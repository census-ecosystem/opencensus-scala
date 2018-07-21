package io.opencensus.scala.http

import io.opencensus.scala.http.ServiceAttributes._
import io.opencensus.scala.http.testSuite.MockSpan
import io.opencensus.trace.AttributeValue._
import org.scalatest.{FlatSpec, Matchers}

class ServiceAttributesSpec extends FlatSpec with Matchers {

  val strValue = stringAttributeValue _

  behavior of "setAttributesForService"

  it should "set the service name only" in {
    val span = new MockSpan("", None)
    setAttributesForService(span, ServiceData("myservice"))

    span.attributes shouldBe Map("service.name" -> strValue("myservice"))
  }

  it should "set the version only" in {
    val span = new MockSpan("", None)
    setAttributesForService(span, ServiceData().setVersion("myversion"))

    span.attributes shouldBe Map("service.version" -> strValue("myversion"))
  }

  it should "set all attributes" in {
    val span = new MockSpan("", None)
    setAttributesForService(span, ServiceData("myservice", "myversion"))

    span.attributes shouldBe Map(
      "service.name"    -> strValue("myservice"),
      "service.version" -> strValue("myversion")
    )
  }
}
