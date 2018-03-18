package com.github.sebruck.opencensus

import io.opencensus.trace.Status
import org.scalatest.compatible.Assertion
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.Future

class TracingSpec extends AsyncFlatSpec with Tracing with Matchers {

  "startSpan" should "start a span" in {
    startSpan("mySpan").getContext.isValid shouldBe true
  }

  "startChildSpan" should "start a span with a parent" in {
    val parent = startSpan("parent")
    startChildSpan("mySpan", parent).getContext.isValid shouldBe true
  }

  "endSpan" should "end a span without throwing an error" in {
    val span = startSpan("span")
    endSpan(span, Status.OK)
    succeed
  }

  behavior of "trace"

  it should "call the given function with a valid span" in {
    trace[Assertion]("span") { span =>
      Future.successful(span.getContext.isValid shouldBe true)
    }
  }

  it should "call the successStatus function with the value in case of success" in {
    var calledWith = 0

    val successStatus = (value: Int) => {
      calledWith = value
      Status.ALREADY_EXISTS
    }

    trace("span", successStatus = successStatus)(_ => Future.successful(42))
      .map(_ => calledWith shouldBe 42)
  }

  it should "call the failureStatus function with the exception in case of failure" in {
    var calledWithMessage = ""

    val failureStatus = (e: Throwable) => {
      calledWithMessage = e.getMessage
      Status.ALREADY_EXISTS
    }

    trace("span", failureStatus = failureStatus)(_ =>
      Future.failed(new Exception("42"))).failed
      .map(_ => calledWithMessage shouldBe "42")
  }

  behavior of "traceChild"

  val parent = startSpan("parent")
  it should "call the given function with a valid span" in {
    traceChild[Assertion]("span", parent) { span =>
      Future.successful(span.getContext.isValid shouldBe true)
    }
  }

  it should "call the successStatus function with the value in case of success" in {
    var calledWith = 0

    val successStatus = (value: Int) => {
      calledWith = value
      Status.ALREADY_EXISTS
    }

    traceChild("span", parent, successStatus = successStatus)(_ =>
      Future.successful(42))
      .map(_ => calledWith shouldBe 42)
  }

  it should "call the failureStatus function with the exception in case of failure" in {
    var calledWithMessage = ""

    val failureStatus = (e: Throwable) => {
      calledWithMessage = e.getMessage
      Status.ALREADY_EXISTS
    }

    traceChild("span", parent, failureStatus = failureStatus)(_ =>
      Future.failed(new Exception("42"))).failed
      .map(_ => calledWithMessage shouldBe "42")
  }
}
