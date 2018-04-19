package com.github.sebruck.opencensus.http4s.propagation

import com.github.sebruck.opencensus.http.propagation.Propagation
import io.opencensus.trace.propagation.TextFormat.{Getter, Setter}
import io.opencensus.trace.{Span, SpanContext}
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Request}

import scala.collection.{immutable, mutable}
import scala.util.Try

private[http4s] trait B3FormatPropagation[F[_]]
    extends Propagation[Header, Request[F]] {

  /** @inheritdoc */
  override def headersWithTracingContext(span: Span): immutable.Seq[Header] = {
    val builder = newHeadersBuilder
    b3Format.inject(span.getContext, builder, Http4sSetter)
    builder.result()
  }

  /** @inheritdoc */
  override def extractContext(request: Request[F]): Try[SpanContext] =
    Try(b3Format.extract(request, Http4sGetter))

  private type HeaderBuilder =
    mutable.Builder[Header, immutable.Seq[Header]]

  private object Http4sSetter extends Setter[HeaderBuilder] {
    override def put(carrier: HeaderBuilder,
                     key: String,
                     value: String): Unit = {
      carrier += Header(key, value)
    }
  }

  private object Http4sGetter extends Getter[Request[F]] {
    override def get(carrier: Request[F], key: String): String =
      carrier.headers
        .get(CaseInsensitiveString(key.toLowerCase))
        .map(_.value)
        .orNull // orNull hurts my hear, but the Getter interface wants it that way
  }

  private def newHeadersBuilder: HeaderBuilder =
    new mutable.Builder[Header, immutable.Seq[Header]] {
      private val b = mutable.ArrayBuffer.newBuilder[Header]

      override def +=(elem: Header): this.type = {
        b += elem
        this
      }

      override def clear(): Unit                   = b.clear()
      override def result(): immutable.Seq[Header] = b.result().toList
    }
}
