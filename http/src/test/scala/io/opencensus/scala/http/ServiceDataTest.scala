package io.opencensus.scala.http

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ServiceDataTest extends AnyFlatSpec with Matchers {

  behavior of "apply"

  it should "be empty" in {
    ServiceData() shouldBe ServiceData(None, None)
  }

  it should "contain only the name" in {
    ServiceData("name") shouldBe ServiceData(Some("name"), None)
  }

  it should "contain the name and the version" in {
    ServiceData("name", "version") shouldBe ServiceData(
      Some("name"),
      Some("version")
    )
  }

  "setName" should "set the name" in {
    ServiceData().setName("name") shouldBe ServiceData(Some("name"), None)
  }

  "setVersion" should "set the version" in {
    ServiceData().setVersion("version") shouldBe ServiceData(
      None,
      Some("version")
    )
  }
}
