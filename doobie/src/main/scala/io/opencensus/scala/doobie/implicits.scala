package io.opencensus.scala.doobie

import cats.Monad
import doobie.ConnectionIO
import doobie.util.transactor.Transactor
import io.opencensus.trace.Span

object implicits {
  implicit class ConnectionTracingOps[A](ma: ConnectionIO[A]) {
    def tracedTransact[M[_]: Monad](
        xa: Transactor[M],
        transactionName: String
    ): M[A] =
      xa.trans.apply(ConnectionIOTracing.traceF(ma, transactionName, None))

    def tracedTransact[M[_]: Monad](
        xa: Transactor[M],
        transactionName: String,
        parentSpan: Span
    ): M[A] =
      xa.trans.apply(
        ConnectionIOTracing.traceF(ma, transactionName, Some(parentSpan))
      )
  }
}
