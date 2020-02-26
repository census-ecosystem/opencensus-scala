package io.opencensus.scala.akka.http

import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Directive1, ExceptionHandler}
import com.typesafe.scalalogging.LazyLogging
import io.opencensus.scala.Tracing
import io.opencensus.scala.akka.http.propagation.AkkaB3FormatPropagation
import io.opencensus.scala.akka.http.trace.EndSpanResponse
import io.opencensus.scala.akka.http.trace.HttpAttributes._
import io.opencensus.scala.http.{HttpAttributes, ServiceAttributes, ServiceData}
import io.opencensus.scala.http.propagation.Propagation
import io.opencensus.trace.{Span, Status}

import scala.util.control.NonFatal

trait TracingDirective extends LazyLogging {

  protected def tracing: Tracing
  protected def propagation: Propagation[HttpHeader, HttpRequest]

  /**
    * Starts a new span and sets a parent context if the request contains valid headers in the b3 format.
    * The span is ended when the request completes or fails with a status code which is suitable
    * to the http response code. The span is passed to the inner route.
    */
  def traceRequest: Directive1[Span] = traceRequest(ServiceData())

  /**
    * Starts a new span and sets a parent context if the request contains valid headers in the b3 format.
    * The span is ended when the request completes or fails with a status code which is suitable
    * to the http response code
    */
  def traceRequestNoSpan: Directive0 = traceRequest(ServiceData()).map(_ => ())

  /**
    * Starts a new span and sets a parent context if the request contains valid headers in the b3 format.
    * The span is ended when the request completes or fails with a status code which is suitable
    * to the http response code. The span is passed to the inner route.
    *
    * Adds the data which is set in serviceData as attributes to the span.
    */
  def traceRequestForService(serviceData: ServiceData): Directive1[Span] =
    traceRequest(serviceData)

  /**
    * Starts a new span and sets a parent context if the request contains valid headers in the b3 format.
    * The span is ended when the request completes or fails with a status code which is suitable
    * to the http response code.
    *
    * Adds the data which is set in serviceData as attributes to the span.
    */
  def traceRequestForServiceNoSpan(serviceData: ServiceData): Directive0 =
    traceRequest(serviceData).map(_ => ())

  private def traceRequest(serviceData: ServiceData): Directive1[Span] =
    extractRequest.flatMap { req =>
      val span = buildSpan(req, serviceData)
      recordSuccess(span) & recordException(span) & provide(span)
    }

  private def buildSpan(req: HttpRequest, serviceData: ServiceData): Span = {
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

    ServiceAttributes.setAttributesForService(span, serviceData)
    HttpAttributes.setAttributesForRequest(span, req)
    span
  }

  private def recordSuccess(span: Span) =
    mapResponse(EndSpanResponse.forServer(tracing, _, span))

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
