package io.opencensus.scala

import io.opencensus.trace.Status

import scala.concurrent.Future
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration.FiniteDuration

class TracingSpec extends AsyncFlatSpec with TracingImpl with Matchers {

  private val deadline = FiniteDuration(1, "millis")
  override protected def config: Config =
    Config(
      TraceConfig(
        exporters = TraceExportersConfig(
          stackdriver =
            StackdriverTraceExporterConfig(enabled = false, "project-id", None),
          logging = LoggingTraceExporterConfig(enabled = false),
          zipkin = ZipkinTraceExporterConfig(enabled = false, "", ""),
          instana = InstanaTraceExporterConfig(enabled = false, ""),
          ocagent = OcAgentTraceExporterConfig(
            enabled = false,
            "project-id",
            "",
            deadline,
            deadline,
            false
          ),
          jaeger = JaegerTraceExporterConfig(
            enabled = false,
            "",
            "",
            deadline
          ),
          datadog =
            DatadogTraceExporterConfig(enabled = false, "", "", "", deadline),
          elasticsearch = ElasticsearchTraceExporterConfig(
            enabled = false,
            "",
            "",
            "",
            "",
            None,
            None,
            deadline
          )
        ),
        samplingProbability = 0.25
      )
    )

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

  "startSpanWithParent" should "start a span with a parent" in {
    val parent = startSpan("parent")
    startSpanWithParent("mySpan", parent).getContext.isValid shouldBe true
  }

  "startSpanWithRemoteParent" should "start a span with a remote parent" in {
    val parent = startSpan("parent")
    startSpanWithRemoteParent("mySpan", parent.getContext).getContext.isValid shouldBe true
  }

  "endSpan" should "end a span without throwing an error" in {
    val span = startSpan("span")
    endSpan(span, Status.OK)
    succeed
  }

  behavior of "trace"

  it should "call the given function with a valid span" in {
    trace("span") { span =>
      Future.successful(span.getContext.isValid shouldBe true)
    }
  }

  it should "call the failureStatus function with the exception in case of failure" in {
    var calledWithMessage = ""

    val failureStatus = (e: Throwable) => {
      calledWithMessage = e.getMessage
      Status.ALREADY_EXISTS
    }

    trace("span", failureStatus = failureStatus)(_ =>
      Future.failed(new Exception("42"))
    ).failed
      .map(_ => calledWithMessage shouldBe "42")
  }

  behavior of "traceChild"

  val parent = startSpan("parent")
  it should "call the given function with a valid span" in {
    traceWithParent("span", parent) { span =>
      Future.successful(span.getContext.isValid shouldBe true)
    }
  }

  it should "call the failureStatus function with the exception in case of failure" in {
    var calledWithMessage = ""

    val failureStatus = (e: Throwable) => {
      calledWithMessage = e.getMessage
      Status.ALREADY_EXISTS
    }

    traceWithParent("span", parent, failureStatus = failureStatus)(_ =>
      Future.failed(new Exception("42"))
    ).failed
      .map(_ => calledWithMessage shouldBe "42")
  }

  "Tracing object" should "successfully initialize with the default reference.conf" in {
    Tracing
    succeed
  }
}
