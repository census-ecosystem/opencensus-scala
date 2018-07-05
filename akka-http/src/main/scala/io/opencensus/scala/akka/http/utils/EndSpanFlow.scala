package io.opencensus.scala.akka.http.utils

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import io.opencensus.scala.Tracing
import io.opencensus.trace.{Span, Status}

class EndSpanFlow[Element](span: Span, tracing: Tracing, status: Status)
    extends GraphStage[FlowShape[Element, Element]] {
  private val in  = Inlet[Element]("in")
  private val out = Outlet[Element]("out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      def onPush(): Unit = push(out, grab(in))
      def onPull(): Unit = pull(in)

      setHandler(in, this)
      setHandler(out, this)

      override def onUpstreamFinish(): Unit = {
        tracing.endSpan(span, status)
        super.onUpstreamFinish()
      }
      override def onUpstreamFailure(ex: Throwable): Unit = {
        tracing.endSpan(span, status)
        super.onUpstreamFailure(ex)
      }
    }

  override val shape = FlowShape(in, out)
}

object EndSpanFlow {
  def apply[Element](
      span: Span,
      tracing: Tracing,
      status: Status
  ): Flow[Element, Element, NotUsed] =
    Flow.fromGraph(new EndSpanFlow(span, tracing, status))
}
