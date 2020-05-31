package io.opencensus.scala

import com.typesafe.scalalogging.LazyLogging
import io.opencensus.scala.trace.exporters.{Logging, Zipkin}

import io.opentelemetry.sdk.trace.Samplers
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.trace.{EndSpanOptions, Span, SpanContext, Status}
import pureconfig.ConfigSource

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import io.opentelemetry.trace.Tracer

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
    * Sets the status of the span
    */
  def setStatus(span: Span, status: Status): Unit

  /**
    * Ends the span
    */
  def endSpan(span: Span): Unit

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
      f: Span => Future[T]
  )(implicit ec: ExecutionContext): Future[T]

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
  def traceWithParent[T](
      name: String,
      parentSpan: Span,
      failureStatus: Throwable => Status = unknownError
  )(f: Span => Future[T])(implicit ec: ExecutionContext): Future[T]
}

private[scala] class TracingImpl(val config: Config) extends Tracing {
  private val tracerProvider = OpenTelemetrySdk.getTracerProvider()
  private val probability =
    tracerProvider
      .getActiveTraceConfig()
      .toBuilder()
      .setSampler(Samplers.probability(config.trace.samplingProbability))
      .build();

  tracerProvider.updateActiveTraceConfig(probability)

  private val tracer: Tracer = tracerProvider.get("TODO")

  /** @inheritdoc */
  override def startSpan(name: String): Span =
    tracer.spanBuilder(name).startSpan()

  /** @inheritdoc */
  override def startSpanWithParent(name: String, parent: Span): Span =
    tracer.spanBuilder(name).setParent(parent).startSpan()

  /** @inheritdoc */
  override def setStatus(span: Span, status: Status): Unit =
    span.setStatus(status)

  /** @inheritdoc */
  override def endSpan(span: Span): Unit = span.end()

  /** @inheritdoc */
  override def startSpanWithRemoteParent(
      name: String,
      parentContext: SpanContext
  ): Span =
    tracer.spanBuilder(name).setParent(parentContext).startSpan()

  /** @inheritdoc */
  override def endSpan(span: Span, status: Status): Unit = {
    span.setStatus(status)
    span.end(EndSpanOptions.builder().build)
  }

  /** @inheritdoc */
  override def trace[T](
      name: String,
      failureStatus: Throwable => Status = unknownError
  )(f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] =
    traceSpan(startSpan(name), failureStatus)(f)

  /** @inheritdoc */
  override def traceWithParent[T](
      name: String,
      parentSpan: Span,
      failureStatus: Throwable => Status = unknownError
  )(f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] =
    traceSpan(startSpanWithParent(name, parentSpan), failureStatus)(f)

  private def traceSpan[T](span: Span, failureStatus: Throwable => Status)(
      f: Span => Future[T]
  )(implicit ec: ExecutionContext): Future[T] = {
    val result = f(span)

    result.onComplete {
      case Success(_) => endSpan(span, Status.OK)
      case Failure(e) => endSpan(span, failureStatus(e))
    }

    result
  }
}

import pureconfig.generic.auto._

object Tracing
    extends TracingImpl(
      ConfigSource.default.at("opencensus-scala").loadOrThrow[Config]
    )
    with LazyLogging {

  if (config.trace.exporters.logging.enabled)
    Logging.init()

  if (config.trace.exporters.zipkin.enabled)
    Zipkin.init(config.trace.exporters.zipkin)

}
