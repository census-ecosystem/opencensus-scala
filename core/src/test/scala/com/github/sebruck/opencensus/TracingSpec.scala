package com.github.sebruck.opencensus

import io.opencensus.trace.Status
import org.scalatest.compatible.Assertion
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.Future

class TracingSpec extends AsyncFlatSpec with Tracing with Matchers {

  override protected def config: Config =
    Config(StackdriverConfig(enabled = false, "project-id", None),
           samplingProbability = 0.25)

  "startSpan" should "start a span" in {
    startSpan("mySpan").getContext.isValid shouldBe true
  }

  it should "take the configured sampling rate into account" in {

    val areSampled = (1 to 1000).map(i => {
      val span = startSpan(i.toString)
      span.getContext.getTraceOptions.isSampled
    })

    val sampled = areSampled.count(identity)

    sampled shouldBe >=(200)
    sampled shouldBe <=(300)
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

  "Tracing object" should "successfully initialize with the default reference.conf" in {
    Tracing
    succeed
  }
}
