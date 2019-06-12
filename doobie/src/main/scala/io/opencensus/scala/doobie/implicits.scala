package io.opencensus.scala.doobie

import cats.Monad
import cats.effect.Bracket
import doobie.ConnectionIO
import doobie.util.transactor.Transactor
import io.opencensus.trace.Span

object implicits {
  implicit class ConnectionTracingOps[A](ma: ConnectionIO[A]) {
    def tracedTransact[M[_]: Monad](
        xa: Transactor[M],
        transactionName: String
    )(implicit ev: Bracket[M, Throwable]): M[A] =
      xa.trans.apply(ConnectionIOTracing.traceF(ma, transactionName, None))

    def tracedTransact[M[_]: Monad](
        xa: Transactor[M],
        transactionName: String,
        parentSpan: Span
    )(implicit ev: Bracket[M, Throwable]): M[A] =
      xa.trans.apply(
        ConnectionIOTracing.traceF(ma, transactionName, Some(parentSpan))
      )
  }
}
