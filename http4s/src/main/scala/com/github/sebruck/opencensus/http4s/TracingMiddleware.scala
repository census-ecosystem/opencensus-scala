package com.github.sebruck.opencensus.http4s

import cats._
import cats.data.{Kleisli, OptionT}
import cats.effect.Effect
import cats.implicits._
import com.github.sebruck.opencensus.Tracing
import com.github.sebruck.opencensus.http.StatusTranslator
import com.github.sebruck.opencensus.http4s.TracingService.{SpanRequest, TracingService}
import io.opencensus.trace.{Span, Status}
import org.http4s.{HttpService, Request, Response}

trait TracingMiddleware {
  protected def tracing: Tracing

  def apply[F[_]: Effect](tracingService: TracingService[F]): HttpService[F] =
    Kleisli { req =>
      val span = buildSpan(req)
      OptionT(
        tracingService(SpanRequest(span, req))
          .map(recordSuccess(span))
          .value
          .adaptError {
            case e =>
              recordException(span)
              e
          })
    }

  def withoutSpan[F[_]: Effect](service: HttpService[F]): HttpService[F] =
    apply(service.local[SpanRequest[F]](spanReq => spanReq.req))

  private def buildSpan[F[_]](req: Request[F]): Span = {
    val span = tracing.startSpan(req.uri.path.toString)
    HttpAttributes.setAttributesForRequest(span, req)
    span
  }

  private def recordSuccess[F[_]](span: Span)(
      response: Response[F]): Response[F] = {
    HttpAttributes.setAttributesForResponse(span, response)
    tracing.endSpan(span, StatusTranslator.translate(response.status.code))
    response
  }

  private def recordException(span: Span): Unit =
    tracing.endSpan(span, Status.INTERNAL)
}

object TracingMiddleware extends TracingMiddleware {
  override protected def tracing: Tracing = Tracing
}

object TracingService {
  case class SpanRequest[F[_]](span: Span, req: Request[F])

  type TracingService[F[_]] =
    Kleisli[OptionT[F, ?], SpanRequest[F], Response[F]]

  def apply[F[_]](pf: PartialFunction[SpanRequest[F], F[Response[F]]])(
      implicit F: Applicative[F]): TracingService[F] =
    Kleisli(
      req =>
        pf.andThen(OptionT.liftF(_))
          .applyOrElse(req, (_: SpanRequest[F]) => OptionT.none))

  object withSpan {
    def unapply[F[_], A](sr: SpanRequest[F]): Option[(Request[F], Span)] =
      Some(sr.req -> sr.span)
  }
}
