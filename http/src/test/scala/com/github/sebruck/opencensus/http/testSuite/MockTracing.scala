package com.github.sebruck.opencensus.http.testSuite

import java.util

import com.github.sebruck.opencensus.Tracing
import io.opencensus.trace._

import scala.concurrent.{ExecutionContext, Future}

class MockTracing extends Tracing {

  type ParentSpanContext = Option[SpanContext]
  @volatile private var _startedSpans       = List.empty[MockSpan]
  @volatile private var _endedSpansStatuses = List.empty[Status]

  def startedSpans: List[MockSpan]     = _startedSpans
  def endedSpansStatuses: List[Status] = _endedSpansStatuses

  override def startSpan(name: String): Span = {
    val span = new MockSpan(name, None)
    _startedSpans = _startedSpans :+ span
    span
  }

  override def startSpanWithParent(name: String, parent: Span): Span = {
    val span = new MockSpan(name, Some(parent.getContext))
    _startedSpans = _startedSpans :+ span
    span
  }

  override def endSpan(span: Span, status: Status): Unit = {
    _endedSpansStatuses = _endedSpansStatuses :+ status
    ()
  }

  override def trace[T](name: String, failureStatus: Throwable => Status)(
      f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] = ???

  override def traceWithParent[T](name: String,
                                  parentSpan: Span,
                                  failureStatus: Throwable => Status)(
      f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] = ???

  override def startSpanWithRemoteParent(name: String,
                                         parentContext: SpanContext): Span = {
    val span = new MockSpan(name, Some(parentContext))
    _startedSpans = _startedSpans :+ span
    span
  }
}

class MockSpan(val name: String, val parentContext: Option[SpanContext])
    extends Span(SpanContext.INVALID, null) {
  import scala.collection.JavaConverters._

  @volatile var attributes = Map[String, AttributeValue]()

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
