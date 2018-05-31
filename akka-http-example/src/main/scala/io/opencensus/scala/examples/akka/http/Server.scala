package io.opencensus.scala.examples.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.scala.akka.http.TracingDirective._
import io.opencensus.trace.AttributeValue
import org.slf4j.bridge.SLF4JBridgeHandler

import scala.util.{Failure, Success}

object Server extends App with LazyLogging {
  // Forward java.util.Logging to slf4j
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()

  implicit val system = ActorSystem()
  implicit val mat    = ActorMaterializer()
  import system.dispatcher

  val routes: Route = traceRequest { span =>
    complete {
      val attrValue = AttributeValue.stringAttributeValue("test")
      span.putAttribute("my-attribute", attrValue)
      "Hello opencensus"
    }
  }

  logger.info("Binding...")
  Http().bindAndHandle(routes, "0.0.0.0", 8080).onComplete {
    case Success(bound) =>
      logger.info(s"Bound to ${bound.localAddress}")
    case Failure(e) =>
      logger.error("Failed to bind", e)
  }
}
