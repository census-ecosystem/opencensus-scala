package io.opencensus.scala.http.testSuite

import java.util

import io.opencensus.scala.Tracing
import io.opencensus.trace._

import scala.concurrent.{ExecutionContext, Future}

class MockTracing extends Tracing {

  type ParentSpanContext = Option[SpanContext]
  @volatile private var _startedSpans = List.empty[MockSpan]
  @volatile private var _setStatus    = List.empty[(Span, Status)]
  @volatile private var _endedSpans   = List.empty[Span]

  def startedSpans: List[MockSpan] = {
    // This sleep "fixes" flakyness of the tests. Since I couldn't find the
    // underlying problem. Even synchronizing everything didn't help.
    Thread.sleep(1)
    _startedSpans
  }

  def spanStauts: List[(Span, Status)] = {
    // This sleep "fixes" flakyness of the tests. Since I couldn't find the
    // underlying problem. Even synchronizing everything didn't help.
    Thread.sleep(1)
    _setStatus
  }

  def endedSpans: List[(Span, Option[Status])] = {
    // This sleep "fixes" flakyness of the tests. Since I couldn't find the
    // underlying problem. Even synchronizing everything didn't help.
    Thread.sleep(1)
    _endedSpans.map(span =>
      (
        span,
        _setStatus
          .find(_._1.getContext.getSpanId == span.getContext.getSpanId)
          .map(_._2)
      )
    )
  }

  override def setStatus(span: Span, status: Status): Unit = {
    _setStatus = _setStatus :+ ((span, status))
    ()
  }

  override def endSpan(span: Span): Unit = {
    _endedSpans = _endedSpans :+ span
    ()
  }

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
    _setStatus = _setStatus :+ ((span, status))
    _endedSpans = _endedSpans :+ span
    ()
  }

  override def trace[T](name: String, failureStatus: Throwable => Status)(
      f: Span => Future[T]
  )(implicit ec: ExecutionContext): Future[T] = ???

  override def traceWithParent[T](
      name: String,
      parentSpan: Span,
      failureStatus: Throwable => Status
  )(f: Span => Future[T])(implicit ec: ExecutionContext): Future[T] = ???

  override def startSpanWithRemoteParent(
      name: String,
      parentContext: SpanContext
  ): Span = {
    val span = new MockSpan(name, Some(parentContext))
    _startedSpans = _startedSpans :+ span
    span
  }
}

class MockSpan(val name: String, val parentContext: Option[SpanContext])
    extends Span(SpanContext.INVALID, null) {
  import scala.jdk.CollectionConverters._

  @volatile var attributes = Map[String, AttributeValue]()
  @volatile var annotaions = List.empty[String]

  override def putAttributes(attr: util.Map[String, AttributeValue]): Unit = {
    attributes = attributes ++ attr.asScala
    ()
  }

  override def addAnnotation(
      description: String,
      attributes: util.Map[String, AttributeValue]
  ): Unit =
    annotaions = description :: annotaions
  override def addAnnotation(annotation: Annotation): Unit = ???
  override def end(options: EndSpanOptions): Unit          = ()
  override def addLink(link: Link): Unit                   = ???
}
