package com.github.sebruck.opencensus.akka.http

import java.util

import com.github.sebruck.opencensus.Tracing
import io.opencensus.trace._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
class MockTracing extends Tracing {

  type ParentSpanContext = Option[SpanContext]
  val startedSpans: ArrayBuffer[(MockSpan, String, ParentSpanContext)] =
    ArrayBuffer[(MockSpan, String, ParentSpanContext)]()
  val endedSpansStatuses: ArrayBuffer[Status] = ArrayBuffer[Status]()

  override def startSpan(name: String): Span = {
    val span = new MockSpan
    startedSpans += ((span, name, None))
    span
  }

  override def startSpanWithParent(name: String, parent: Span): Span = {
    val span = new MockSpan
    startedSpans += ((span, name, Some(parent.getContext)))
    span
  }

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
    val span = new MockSpan
    startedSpans += ((span, name, Some(parentContext)))
    span
  }
}

class MockSpan extends Span(SpanContext.INVALID, null) {
  import scala.collection.JavaConverters._

  var attributes = Map[String, AttributeValue]()

  override def putAttributes(attr: util.Map[String, AttributeValue]): Unit = {
    attributes = attributes ++ attr.asScala
    ()
  }

  override def addAnnotation(
      description: String,
      attributes: util.Map[String, AttributeValue]): Unit  = ???
  override def addAnnotation(annotation: Annotation): Unit = ???
  override def end(options: EndSpanOptions): Unit          = ()
  override def addLink(link: Link): Unit                   = ???
}
