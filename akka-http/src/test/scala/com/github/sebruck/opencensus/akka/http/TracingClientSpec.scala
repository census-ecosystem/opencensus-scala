package com.github.sebruck.opencensus.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.sebruck.opencensus.Tracing
import com.github.sebruck.opencensus.http.Propagation
import com.github.sebruck.opencensus.http.testSuite.MockTracing
import io.opencensus.trace.Span

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class TracingClientSpec extends ClientSpec {

  implicit val system = ActorSystem()
  implicit val mat    = ActorMaterializer()

  "traceRequest function based" should behave like testClient(
    clientWithMockFunction _)

  "traceRequest host flow based" should behave like testClient(
    clientWithMockHostFlow _)

  "traceRequest connection flow based" should behave like testClient(
    clientWithMockConnectionFlow _)

  def clientWithMockFunction() = {
    val mockTracing = new MockTracing
    val client = new TracingClient {
      override protected val tracing: Tracing = mockTracing
      override protected val propagation: Propagation[HttpHeader, HttpRequest] =
        AkkaMockPropagation
      override implicit protected val ec: ExecutionContext = global
    }

    (client.traceRequest(_: HttpRequest => Future[HttpResponse], _: Span),
     mockTracing)
  }

  def clientWithMockConnectionFlow() = {
    val mockTracing = new MockTracing
    val client = new TracingClient {
      override protected val tracing: Tracing = mockTracing
      override protected val propagation: Propagation[HttpHeader, HttpRequest] =
        AkkaMockPropagation
      override implicit protected val ec: ExecutionContext = global
    }

    val clientFunction =
      (doRequest: HttpRequest => Future[HttpResponse], parentSpan: Span) =>
        (request: HttpRequest) => {
          val flow = Flow[HttpRequest]
            .mapAsync(1)(doRequest)

          val enrichedFlow = client.traceRequestForConnection(flow, parentSpan)

          Source
            .single(request)
            .via(enrichedFlow)
            .runWith(Sink.head)
      }

    (clientFunction, mockTracing)
  }

  def clientWithMockHostFlow() = {
    val mockTracing = new MockTracing
    val client = new TracingClient {
      override protected val tracing: Tracing = mockTracing
      override protected val propagation: Propagation[HttpHeader, HttpRequest] =
        AkkaMockPropagation
      override implicit protected val ec: ExecutionContext = global
    }

    val clientFunction =
      (doRequest: HttpRequest => Future[HttpResponse], parentSpan: Span) =>
        (request: HttpRequest) => {
          val flow = Flow[(HttpRequest, Unit)]
            .mapAsync(1) {
              case (r, _) =>
                doRequest(r)
                  .map[(Try[HttpResponse], Unit)](response =>
                    (Success(response), ()))
                  .recover {
                    case NonFatal(e) => (Failure(e), ())
                  }
            }

          val enrichedFlow = client.traceRequestForPool(flow, parentSpan)

          Source
            .single((request, ()))
            .via(enrichedFlow)
            .runWith(Sink.head)
            .map {
              case (Success(response), _) => response
              case (Failure(error), _)    => throw error
            }
      }

    (clientFunction, mockTracing)
  }
}
