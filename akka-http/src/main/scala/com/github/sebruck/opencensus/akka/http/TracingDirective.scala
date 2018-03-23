package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, ExceptionHandler}
import com.github.sebruck.opencensus.Tracing
import com.github.sebruck.opencensus.akka.http.propagation.B3FormatPropagation
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.trace.{Span, Status}

import scala.util.control.NonFatal

trait TracingDirective extends LazyLogging {

  protected def tracing: Tracing
  protected def propagation: Propagation

  val traceRequest: Directive1[Span] =
    extractRequest.flatMap { req =>
      val span = buildSpan(req)
      recordSuccess(span) & recordException(span) & provide(span)
    }

  private def buildSpan(req: HttpRequest): Span = {
    val name = req.uri.path.toString()

    propagation
      .extractContext(req)
      .fold(
        { error =>
          logger.warn("Extracting of parent context failed", error)
          tracing.startSpan(name)
        },
        tracing.startSpanWithRemoteParent(name, _)
      )
  }

  private def recordSuccess(span: Span) = mapResponse { response =>
    tracing.endSpan(span, StatusTranslator.translate(response.status))
    response
  }

  private def recordException(span: Span) =
    handleExceptions(ExceptionHandler {
      case NonFatal(ex) =>
        tracing.endSpan(span, Status.INTERNAL)
        throw ex
    })
}

object TracingDirective extends TracingDirective {
  override protected def tracing: Tracing         = Tracing
  override protected def propagation: Propagation = B3FormatPropagation
}
