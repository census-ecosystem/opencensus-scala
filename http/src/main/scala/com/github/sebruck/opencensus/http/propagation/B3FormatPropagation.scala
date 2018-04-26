package com.github.sebruck.opencensus.http.propagation

import io.opencensus.trace.propagation.TextFormat.{Getter, Setter}
import io.opencensus.trace.{Span, SpanContext}

import scala.collection.{immutable, mutable}
import scala.util.Try

trait B3FormatPropagation[Header, Request]
    extends Propagation[Header, Request] {

  def headerValue(req: Request, key: String): Option[String]
  def createHeader(key: String, value: String): Header

  /** @inheritdoc */
  override def headersWithTracingContext(span: Span): immutable.Seq[Header] = {
    val builder = newHeadersBuilder
    b3Format.inject(span.getContext, builder, HeaderSetter)
    builder.result()
  }

  /** @inheritdoc */
  override def extractContext(request: Request): Try[SpanContext] =
    Try(b3Format.extract(request, HeaderGetter))

  private type HttpHeaderBuilder =
    mutable.Builder[Header, immutable.Seq[Header]]

  private object HeaderSetter extends Setter[HttpHeaderBuilder] {
    override def put(carrier: HttpHeaderBuilder,
                     key: String,
                     value: String): Unit = {
      carrier += createHeader(key, value)
    }
  }

  private object HeaderGetter extends Getter[Request] {
    override def get(carrier: Request, key: String): String =
      headerValue(carrier, key).orNull
  }

  private def newHeadersBuilder: HttpHeaderBuilder =
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
