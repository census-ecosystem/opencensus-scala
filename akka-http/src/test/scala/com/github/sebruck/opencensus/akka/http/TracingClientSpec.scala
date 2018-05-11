package com.github.sebruck.opencensus.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.sebruck.opencensus.Tracing
import com.github.sebruck.opencensus.http.propagation.Propagation
import com.github.sebruck.opencensus.http.testSuite.MockTracing
import io.opencensus.trace.Span
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class TracingClientSpec extends ClientSpec with BeforeAndAfterAll {

  implicit val system = ActorSystem()
  implicit val mat    = ActorMaterializer()

  "traceRequest function based with parent" should behave like testClient(
    clientWithMockFunction(withParent = true) _,
    withParent = true)

  "traceRequest function based without parent" should behave like testClient(
    clientWithMockFunction(withParent = false) _,
    withParent = false)

  "traceRequest host flow based with parent" should behave like testClient(
    clientWithMockHostFlow(withParent = true) _,
    withParent = true)

  "traceRequest host flow based without parent" should behave like testClient(
    clientWithMockHostFlow(withParent = false) _,
    withParent = false)

  "traceRequest connection flow based with parent" should behave like testClient(
    clientWithMockConnectionFlow(withParent = true) _,
    withParent = true)

  "traceRequest connection flow based without parent" should behave like testClient(
    clientWithMockConnectionFlow(withParent = false) _,
    withParent = false)

  def clientWithMockFunction(withParent: Boolean)() = {
    val mockTracing = new MockTracing
    val client = new TracingClient {
      override protected val tracing: Tracing = mockTracing
      override protected val propagation: Propagation[HttpHeader, HttpRequest] =
        AkkaMockPropagation
      override implicit protected val ec: ExecutionContext = executionContext
    }

    def fun =
      if (withParent)
        client.traceRequest(_: HttpRequest => Future[HttpResponse], _: Span)
      else
        ((doRequest, _) => client.traceRequest(doRequest)): Client

    (fun, mockTracing)
  }

  def clientWithMockConnectionFlow(withParent: Boolean)() = {
    val mockTracing = new MockTracing
    val client = new TracingClient {
      override protected val tracing: Tracing = mockTracing
      override protected val propagation: Propagation[HttpHeader, HttpRequest] =
        AkkaMockPropagation
      override implicit protected val ec: ExecutionContext = executionContext
    }

    val clientFunction =
      (doRequest: HttpRequest => Future[HttpResponse], parentSpan: Span) =>
        (request: HttpRequest) => {
          val flow = Flow[HttpRequest]
            .mapAsync(1)(doRequest)

          val enrichedFlow =
            if (withParent)
              client.traceRequestForConnection(flow, parentSpan)
            else
              client.traceRequestForConnection(flow)

          Source
            .single(request)
            .via(enrichedFlow)
            .runWith(Sink.head)
      }

    (clientFunction, mockTracing)
  }

  def clientWithMockHostFlow(withParent: Boolean)() = {
    val mockTracing = new MockTracing
    val client = new TracingClient {
      override protected val tracing: Tracing = mockTracing
      override protected val propagation: Propagation[HttpHeader, HttpRequest] =
        AkkaMockPropagation
      override implicit protected val ec: ExecutionContext = executionContext
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

          val enrichedFlow =
            if (withParent)
              client.traceRequestForPool(flow, parentSpan)
            else
              client.traceRequestForPool(flow)

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

  override protected def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
}
