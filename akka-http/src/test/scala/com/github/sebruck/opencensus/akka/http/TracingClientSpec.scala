package com.github.sebruck.opencensus.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.sebruck.opencensus.Tracing
import io.opencensus.trace.Span

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class TracingClientSpec extends ClientSpec {

  implicit val system = ActorSystem()
  implicit val mat    = ActorMaterializer()

  "traceRequest function based" should behave like testClient(
    clientWithMockFunction)

  "traceRequest flow based" should behave like testClient(clientWithMockFlow)

  def clientWithMockFunction() = {
    val mockTracing = new MockTracing
    val client = new TracingClient {
      override protected val tracing: Tracing              = mockTracing
      override protected val propagation: Propagation      = MockPropagation
      override implicit protected val ec: ExecutionContext = global
    }

    (client.traceRequest(_: HttpRequest => Future[HttpResponse], _: Span),
     mockTracing)
  }

  def clientWithMockFlow() = {
    val mockTracing = new MockTracing
    val client = new TracingClient {
      override protected val tracing: Tracing              = mockTracing
      override protected val propagation: Propagation      = MockPropagation
      override implicit protected val ec: ExecutionContext = global
    }

    val clientFunction =
      (doRequest: HttpRequest => Future[HttpResponse], parentSpan: Span) =>
        (request: HttpRequest) => {
          val flow = Flow[HttpRequest]
            .mapAsync(1)(doRequest)

          val enrichedFlow = client.traceRequest(flow, parentSpan)

          Source
            .single(request)
            .via(enrichedFlow)
            .runWith(Sink.head)
      }

    (clientFunction, mockTracing)
  }
}
