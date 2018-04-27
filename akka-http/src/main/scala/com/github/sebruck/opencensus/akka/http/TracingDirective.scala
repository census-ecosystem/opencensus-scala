package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, ExceptionHandler}
import com.github.sebruck.opencensus.Tracing
import com.github.sebruck.opencensus.akka.http.propagation.AkkaB3FormatPropagation
import com.github.sebruck.opencensus.akka.http.trace.HttpAttributes._
import com.github.sebruck.opencensus.http.{HttpAttributes, StatusTranslator}
import com.github.sebruck.opencensus.http.propagation.Propagation
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.trace.{Span, Status}

import scala.util.control.NonFatal

trait TracingDirective extends LazyLogging {

  protected def tracing: Tracing
  protected def propagation: Propagation[HttpHeader, HttpRequest]

  /**
    * Starts a new span and sets a parent context if the request contains valid headers in the b3 format.
    * The span is ended when the request completes or fails with a status code which is suitable
    * to the http response code
    */
  val traceRequest: Directive1[Span] =
    extractRequest.flatMap { req =>
      val span = buildSpan(req)
      recordSuccess(span) & recordException(span) & provide(span)
    }

  private def buildSpan(req: HttpRequest): Span = {
    val name = req.uri.path.toString()

    val span = propagation
      .extractContext(req)
      .fold(
        { error =>
          logger.debug("Extracting of parent context failed", error)
          tracing.startSpan(name)
        },
        tracing.startSpanWithRemoteParent(name, _)
      )

    HttpAttributes.setAttributesForRequest(span, req)
    span
  }

  private def recordSuccess(span: Span) = mapResponse { response =>
    HttpAttributes.setAttributesForResponse(span, response)
    tracing.endSpan(span,
                    StatusTranslator.translate(response.status.intValue()))
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
  override protected def propagation: Propagation[HttpHeader, HttpRequest] =
    AkkaB3FormatPropagation
}
