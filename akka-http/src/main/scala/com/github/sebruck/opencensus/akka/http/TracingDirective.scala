package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, ExceptionHandler}
import com.github.sebruck.opencensus.Tracing
import io.opencensus.trace.{Span, Status}

import scala.util.control.NonFatal

trait TracingDirective {
  protected def tracing: Tracing

  val traceRequest: Directive1[Span] =
    extractRequest.flatMap { req =>
      val name = req.uri.path.toString()
      val span = tracing.startSpan(name)
      recordSuccess(span) & recordException(span) & provide(span)
    }

  private def recordSuccess(span: Span) = mapResponse { response =>
    tracing.endSpan(span, Status.OK)
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
  override protected def tracing: Tracing = Tracing
}
