package com.github.sebruck.opencensus

import com.typesafe.scalalogging.LazyLogging
import io.opencensus.trace.samplers.Samplers
import pureconfig.loadConfigOrThrow
import io.opencensus.trace.{
  EndSpanOptions,
  Span,
  SpanBuilder,
  Status,
  Tracing => OpencensusTracing
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Tracing extends LazyLogging {
  private val config = loadConfigOrThrow[Config]("opencensus-scala")

  if (config.stackdriver.enabled) {
    Stackdriver.init(config.stackdriver)
  }

  private val tracer = OpencensusTracing.getTracer

  private val unknownError = (_: Throwable) => Status.UNKNOWN

  def traceChild[T](name: String,
                    parentSpan: Span,
                    successStatus: T => Status = (_: T) => Status.OK,
                    failureStatus: Throwable => Status = unknownError)(
      implicit ec: ExecutionContext): (Span => Future[T]) => Future[T] = { f =>
    val span = startChildSpan(name, parentSpan)

    val result = f(span)

    result.onComplete {
      case Success(value) => endSpan(span, successStatus(value))
      case Failure(e)     => endSpan(span, failureStatus(e))
    }

    result
  }

  def startSpan(name: String): Span = buildSpan(tracer.spanBuilder(name))

  def startChildSpan(name: String, parent: Span): Span =
    buildSpan(tracer.spanBuilderWithExplicitParent(name, parent))

  private def buildSpan(builder: SpanBuilder): Span = {
    builder
      .setSampler(Samplers.alwaysSample())
      .startSpan()
  }

  def endSpan(span: Span, status: Status): Unit =
    span.end(EndSpanOptions.builder().setStatus(status).build())
}
