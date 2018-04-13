package com.github.sebruck.opencensus.akka.http.propagation

import com.github.sebruck.opencensus.akka.http.Propagation
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import io.opencensus.trace.propagation.TextFormat.{Getter, Setter}
import io.opencensus.trace.{Span, SpanContext, Tracing => OpencensusTracing}

import scala.collection.{immutable, mutable}
import scala.util.Try

trait B3FormatPropagation extends Propagation {
  private val b3Format = OpencensusTracing.getPropagationComponent.getB3Format

  /** @inheritdoc */
  override def headersWithTracingContext(
      span: Span): immutable.Seq[HttpHeader] = {
    val builder = newHeadersBuilder
    b3Format.inject(span.getContext, builder, AkkaHttpSetter)
    builder.result()
  }

  /** @inheritdoc */
  override def extractContext(request: HttpRequest): Try[SpanContext] =
    Try(b3Format.extract(request, AkkaHttpGetter))

  private type HttpHeaderBuilder =
    mutable.Builder[HttpHeader, immutable.Seq[HttpHeader]]

  private object AkkaHttpSetter extends Setter[HttpHeaderBuilder] {
    override def put(carrier: HttpHeaderBuilder,
                     key: String,
                     value: String): Unit = {
      carrier += RawHeader(fixAscii(key), value)
    }
  }

  private object AkkaHttpGetter extends Getter[HttpRequest] {
    override def get(carrier: HttpRequest, key: String): String =
      carrier.headers
        .find(_.lowercaseName() == fixAscii(key).toLowerCase)
        .map(_.value())
        .orNull // orNull hurts my hear, but the Getter interface wants it that way
  }

  // Since this fix is not released yet, we have to replace it ourselfes
  // https://github.com/census-instrumentation/opencensus-java/commit/3683e6263dbbada41a9f8f72845c19222d05b4fc#diff-4d643fa125329a19427eaf90e317a27b
  private def fixAscii(key: String): String =
    key.replace('─', '-')

  private def newHeadersBuilder: HttpHeaderBuilder =
    new mutable.Builder[HttpHeader, immutable.Seq[HttpHeader]] {
      private val b = mutable.ArrayBuffer.newBuilder[HttpHeader]

      override def +=(elem: HttpHeader): this.type = {
        b += elem
        this
      }

      override def clear(): Unit                       = b.clear()
      override def result(): immutable.Seq[HttpHeader] = b.result().toList
    }
}

object B3FormatPropagation extends B3FormatPropagation
