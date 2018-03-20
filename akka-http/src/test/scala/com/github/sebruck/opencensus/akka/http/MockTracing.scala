package com.github.sebruck.opencensus.akka.http

import com.github.sebruck.opencensus.Tracing
import io.opencensus.trace.{BlankSpan, Span, Status}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
class MockTracing extends Tracing {

  val startedSpans: ArrayBuffer[String]       = ArrayBuffer[String]()
  val endedSpansStatuses: ArrayBuffer[Status] = ArrayBuffer[Status]()

  override def startSpan(name: String): Span = {
    startedSpans += name
    BlankSpan.INSTANCE
  }

  override def startChildSpan(name: String, parent: Span): Span = ???

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

}
