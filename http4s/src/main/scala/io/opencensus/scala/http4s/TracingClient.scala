package io.opencensus.scala.http4s

import cats.effect.{Effect, Resource}
import cats.implicits._
import io.opencensus.scala.Tracing
import io.opencensus.scala.http.propagation.Propagation
import io.opencensus.scala.http.{HttpAttributes => BaseHttpAttributes}
import io.opencensus.scala.http4s.HttpAttributes._
import io.opencensus.scala.http4s.TracingUtils.recordResponse
import io.opencensus.scala.http4s.propagation.Http4sFormatPropagation
import io.opencensus.trace.{Span, Status}
import org.http4s.client.Client
import org.http4s.{Header, Request, Response}

abstract class TracingClient[F[_]: Effect] {

  protected val tracing: Tracing
  protected val propagation: Propagation[Header, Request[F]]

  /**
    * Enriches the `Client[F]` by tracing and propagation of the SpanContext via http headers.
    *
    * @param parentSpan the current span which will act as parent of the new span if given
    */
  def trace(client: Client[F], parentSpan: Option[Span] = None): Client[F] = {
    val tracedOpen: Request[F] => Resource[F, Response[F]] =
      req =>
        for {
          span <- Resource.liftF(startSpan(parentSpan, req))
          enrichedReq = addTraceHeaders(req, span)
          res <- client
            .run(enrichedReq)
            .onError(traceError(span).andThen(x => Resource.liftF(x)))
        } yield recordResponse(span, tracing)(res)

    Client(tracedOpen)
  }

  private def traceError(span: Span): PartialFunction[Throwable, F[Unit]] = {
    case _ => recordException(span)
  }

  private def startSpan(parentSpan: Option[Span], req: Request[F]) =
    Effect[F].delay(startAndEnrichSpan(req, parentSpan))

  private def startAndEnrichSpan(
      req: Request[F],
      parentSpan: Option[Span]
  ): Span = {
    val name = req.uri.path.toString
    val span = parentSpan.fold(tracing.startSpan(name))(
      span => tracing.startSpanWithParent(name, span)
    )
    BaseHttpAttributes.setAttributesForRequest(span, req)
    span
  }

  private def addTraceHeaders(request: Request[F], span: Span): Request[F] =
    request.withHeaders(
      request.headers.put(propagation.headersWithTracingContext(span): _*)
    )

  private def recordException(span: Span) =
    Effect[F].delay(tracing.endSpan(span, Status.INTERNAL))
}

object TracingClient {
  def apply[F[_]: Effect]: TracingClient[F] =
    new TracingClient[F] {
      override protected val tracing: Tracing = Tracing
      override protected val propagation: Propagation[Header, Request[F]] =
        new Http4sFormatPropagation[F] {}
    }
}
