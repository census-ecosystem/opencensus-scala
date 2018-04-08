package com.github.sebruck.opencensus

import com.github.sebruck.opencensus.trace.exporters.Stackdriver
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.trace.samplers.Samplers
import pureconfig.loadConfigOrThrow
import io.opencensus.trace.{
  EndSpanOptions,
  Span,
  SpanBuilder,
  SpanContext,
  Status,
  Tracing => OpencensusTracing
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait Tracing {

  protected val unknownError = (_: Throwable) => Status.UNKNOWN

  /**
    * Starts a root span
    */
  def startSpan(name: String): Span

  /**
    * Starts a child span of the given parent
    */
  def startSpanWithParent(name: String, parent: Span): Span

  /**
    * Starts a child span of the remote span
    */
  def startSpanWithRemoteParent(name: String, parentContext: SpanContext): Span

  /**
    * Ends the span with the given status
    */
  def endSpan(span: Span, status: Status): Unit

  /**
    * Starts a new root span before executing the given function.
    *
    * When the Future which is returned by the provided function completes successfully, the span will be ended
    * with the status returned by `successStatus` otherwise with the status returned by `failureStatus`.
    *
    * @param name the name of the created span
    * @param failureStatus function defining the status with which the Span will be ended in case of failure
    * @param f an unary function which parameter is the action which should be traced. The newly created span is given
    *         as a parameter in case it is needed as parent reference for further spans.
    * @return the return value of f
    */
  def trace[T](name: String, failureStatus: Throwable => Status = unknownError)(
      f: Span => Future[T])(implicit ec: ExecutionContext): Future[T]

  /**
    * Starts a new child span of the given parent span before executing the given function.
    *
    * When the Future which is returned by the provided function completes successfully, the span will be ended
    * with the status returned by `successStatus` otherwise with the status returned by `failureStatus`.
    *
    * @param name the name of the created span
    * @param parentSpan the parent span
    * @param failureStatus function defining the status with which the Span will be ended in case of failure
    * @param f an unary function which parameter is the action which should be traced. The newly created span is given
    *         as a parameter in case it is needed as parent reference for further spans.
    * @return the return value of f
    */
  def traceChild[T](name: String,
                    parentSpan: Span,
                    failureStatus: Throwable => Status = unknownError)(
      f: Span => Future[T])(implicit ec: ExecutionContext): Future[T]
}

trait TracingImpl extends Tracing {
  private val tracer = OpencensusTracing.getTracer
  protected def config: Config

  /** @inheritdoc */
  override def startSpan(name: String): Span =
    buildSpan(tracer.spanBuilder(name))

  /** @inheritdoc */
  override def startSpanWithParent(name: String, parent: Span): Span =
    buildSpan(tracer.spanBuilderWithExplicitParent(name, parent))

  /** @inheritdoc */
  override def startSpanWithRemoteParent(name: String,
                                         parentContext: SpanContext): Span =
    buildSpan(tracer.spanBuilderWithRemoteParent(name, parentContext))

  /** @inheritdoc */
  override def endSpan(span: Span, status: Status): Unit =
    span.end(EndSpanOptions.builder().setStatus(status).build())

  /** @inheritdoc */
  override def trace[T](name: String,
                        failureStatus: Throwable => Status = unknownError)(
      f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] =
    traceSpan(startSpan(name), failureStatus)(f)

  /** @inheritdoc */
  override def traceChild[T](name: String,
                             parentSpan: Span,
                             failureStatus: Throwable => Status = unknownError)(
      f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] =
    traceSpan(startSpanWithParent(name, parentSpan), failureStatus)(f)

  private def traceSpan[T](span: Span, failureStatus: Throwable => Status)(
      f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val result = f(span)

    result.onComplete {
      case Success(_) => endSpan(span, Status.OK)
      case Failure(e) => endSpan(span, failureStatus(e))
    }

    result
  }

  private def buildSpan(builder: SpanBuilder): Span = {
    builder
      .setSampler(Samplers.probabilitySampler(config.trace.samplingProbability))
      .startSpan()
  }
}

object Tracing extends TracingImpl with LazyLogging {
  override protected val config = loadConfigOrThrow[Config]("opencensus-scala")

  if (config.trace.exporters.stackdriver.enabled) {
    Stackdriver.init(config.trace.exporters.stackdriver)
  }
}
