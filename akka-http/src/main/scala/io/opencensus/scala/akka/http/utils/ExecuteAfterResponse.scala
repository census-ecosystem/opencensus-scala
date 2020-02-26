package io.opencensus.scala.akka.http.utils

import akka.NotUsed
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

object ExecuteAfterResponse {

  private class AfterResponseFlow[Element](
      onFinish: () => Unit,
      onFailure: Throwable => Unit
  ) extends GraphStage[FlowShape[Element, Element]] {
    private val in  = Inlet[Element]("in")
    private val out = Outlet[Element]("out")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with InHandler with OutHandler {
        def onPush(): Unit = push(out, grab(in))
        def onPull(): Unit = pull(in)

        setHandler(in, this)
        setHandler(out, this)

        override def onUpstreamFinish(): Unit = {
          onFinish()
          super.onUpstreamFinish()
        }
        override def onUpstreamFailure(ex: Throwable): Unit = {
          onFailure(ex)
          super.onUpstreamFailure(ex)
        }
      }

    override val shape = FlowShape(in, out)
  }

  private object AfterResponseFlow {
    def apply[Element](
        onFinish: () => Unit,
        onFailure: Throwable => Unit
    ): Flow[Element, Element, NotUsed] =
      Flow.fromGraph(new AfterResponseFlow(onFinish, onFailure))
  }

  def onComplete(
      response: HttpResponse,
      onFinish: () => Unit,
      onFailure: Throwable => Unit
  ): HttpResponse = {

    response.copy(
      entity = if (response.status.allowsEntity) {
        response.entity.transformDataBytes(
          AfterResponseFlow(onFinish, onFailure)
        )
      } else {
        onFinish()
        HttpEntity.Empty
      }
    )
  }
}
