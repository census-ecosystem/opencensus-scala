package io.opencensus.scala.http

case class ServiceData(name: Option[String], version: Option[String]) {
  def setName(name: String): ServiceData = this.copy(name = Some(name))
  def setVersion(version: String): ServiceData =
    this.copy(version = Some(version))
}

object ServiceData {
  def apply(): ServiceData             = ServiceData(None, None)
  def apply(name: String): ServiceData = ServiceData(Some(name), None)
  def apply(name: String, version: String): ServiceData =
    ServiceData(Some(name), Some(version))
}
