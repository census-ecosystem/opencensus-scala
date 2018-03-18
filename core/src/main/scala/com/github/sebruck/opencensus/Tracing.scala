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

  private val tracer       = OpencensusTracing.getTracer
  private val unknownError = (_: Throwable) => Status.UNKNOWN
  private def ok[T]        = (_: T) => Status.OK

  def traceChild[T](name: String,
                    parentSpan: Span,
                    successStatus: T => Status = ok,
                    failureStatus: Throwable => Status = unknownError)(
      implicit ec: ExecutionContext): (Span => Future[T]) => Future[T] =
    traceSpan(startChildSpan(name, parentSpan), successStatus, failureStatus)

  def trace[T](name: String,
               successStatus: T => Status = ok,
               failureStatus: Throwable => Status = unknownError)(
      implicit ec: ExecutionContext): (Span => Future[T]) => Future[T] =
    traceSpan(startSpan(name), successStatus, failureStatus)

  def startSpan(name: String): Span = buildSpan(tracer.spanBuilder(name))

  def startChildSpan(name: String, parent: Span): Span =
    buildSpan(tracer.spanBuilderWithExplicitParent(name, parent))

  def endSpan(span: Span, status: Status): Unit =
    span.end(EndSpanOptions.builder().setStatus(status).build())

  private def traceSpan[T](span: Span,
                           successStatus: T => Status,
                           failureStatus: Throwable => Status)(
      implicit ec: ExecutionContext): (Span => Future[T]) => Future[T] = { f =>
    val result = f(span)

    result.onComplete {
      case Success(value) => endSpan(span, successStatus(value))
      case Failure(e)     => endSpan(span, failureStatus(e))
    }

    result
  }

  private def buildSpan(builder: SpanBuilder): Span = {
    builder
      .setSampler(Samplers.alwaysSample())
      .startSpan()
  }
}
