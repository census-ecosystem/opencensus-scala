package io.opencensus.scala.examples.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.opencensus.scala.akka.http.TracingClient
import org.slf4j.bridge.SLF4JBridgeHandler

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Client extends App {
  // Forward java.util.Logging to slf4j
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()

  implicit val system = ActorSystem()
  implicit val mat    = ActorMaterializer()
  import system.dispatcher

  def await[T](f: Future[T]) = Await.result(f, 3.seconds)

  // Request level client
  val pipeling = Http().singleRequest(_: HttpRequest)
  val r1 = await {
    TracingClient
      .traceRequest(pipeling)(HttpRequest(uri = "http://localhost:8080"))
      .flatMap(_.entity.toStrict(1.second))
      .map(_.data.utf8String)
  }
  println(r1)

  // Host level client
  val pool     = Http().cachedHostConnectionPool[Unit]("localhost", 8080)
  val hostFlow = TracingClient.traceRequestForPool(pool)

  val r2 = await {
    Source
      .single(HttpRequest(uri = "/"))
      .map((_, ()))
      .via(hostFlow)
      .map(_._1)
      .flatMapConcat {
        case Success(response) => response.entity.dataBytes
        case Failure(e)        => throw e
      }
      .map(_.utf8String)
      .runWith(Sink.head)
  }
  println(r2)

  // Connection level client
  val connection     = Http().outgoingConnection("localhost", 8080)
  val connectionFlow = TracingClient.traceRequestForConnection(connection)

  val r3 = await {
    Source
      .single(HttpRequest(uri = "/"))
      .via(connectionFlow)
      .flatMapConcat(_.entity.dataBytes)
      .map(_.utf8String)
      .runWith(Sink.head)
  }
  println(r3)
}
