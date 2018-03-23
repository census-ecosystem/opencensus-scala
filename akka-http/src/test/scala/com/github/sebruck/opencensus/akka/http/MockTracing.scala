package com.github.sebruck.opencensus.akka.http

import com.github.sebruck.opencensus.Tracing
import io.opencensus.trace.{BlankSpan, Span, SpanContext, Status}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
class MockTracing extends Tracing {

  type ParentSpanContext = Option[SpanContext]
  val startedSpans: ArrayBuffer[(String, ParentSpanContext)] =
    ArrayBuffer[(String, ParentSpanContext)]()
  val endedSpansStatuses: ArrayBuffer[Status] = ArrayBuffer[Status]()

  override def startSpan(name: String): Span = {
    startedSpans += ((name, None))
    BlankSpan.INSTANCE
  }

  override def startSpanWithParent(name: String, parent: Span): Span = ???

  override def endSpan(span: Span, status: Status): Unit = {
    endedSpansStatuses += status
    ()
  }

  override def trace[T](name: String, failureStatus: Throwable => Status)(
      f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] = ???

  override def traceChild[T](name: String,
                             parentSpan: Span,
                             failureStatus: Throwable => Status)(
      f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] = ???

  override def startSpanWithRemoteParent(name: String,
                                         parentContext: SpanContext): Span = {
    startedSpans += ((name, Some(parentContext)))
    BlankSpan.INSTANCE
  }
}
