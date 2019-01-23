package io.opencensus.scala.doobie

import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{Bracket, Sync}
import doobie.free.connection.ConnectionIO
import io.opencensus.scala.Tracing
import io.opencensus.scala.doobie.FTracing.FBracket
import io.opencensus.trace.{Span, Status}
import cats.syntax.flatMap._
import cats.syntax.functor._

abstract class FTracing[F[_]: Sync: FBracket] {

  protected val tracing: Tracing

  private def fTrace(name: String, parentSpan: Option[Span]): F[Span] =
    Sync[F].delay(
      parentSpan.fold(tracing.startSpan(name))(
        span => tracing.startSpanWithParent(name, span)
      )
    )

  private def fStop(span: Span): F[Unit] =
    Sync[F].delay(tracing.endSpan(span, Status.OK))

  private def fStopError(span: Span): F[Unit] =
    Sync[F].delay(tracing.endSpan(span, Status.INTERNAL))

  private def fStopCanceled(span: Span): F[Unit] =
    Sync[F].delay(tracing.endSpan(span, Status.CANCELLED))

  def traceF[A](co: F[A], name: String, parentSpan: Option[Span]): F[A] =
    for {
      startedSpan <- fTrace(name, parentSpan)
      result <- Bracket[F, Throwable].guaranteeCase(co) {
        case Completed => fStop(startedSpan)
        case Error(_)  => fStopError(startedSpan)
        case Canceled  => fStopCanceled(startedSpan)
      }
    } yield result
}

object FTracing {
  type FBracket[F[_]] = Bracket[F, Throwable]
}

object ConnectionIOTracing extends FTracing[ConnectionIO] {
  override protected val tracing: Tracing = Tracing
}
