package com.github.sebruck.opencensus.http

import io.opencensus.trace.Status
import io.opencensus.trace.Status._

object StatusTranslator {

  /**
    * According to
    * https://github.com/census-instrumentation/opencensus-specs/blob/master/trace/HTTP.md#status
    */
  def translate(httpStatus: Int): Status = httpStatus match {
    case code if code > 0 && code < 200    => UNKNOWN
    case code if code >= 200 && code < 400 => OK
    case 400                               => INVALID_ARGUMENT
    case 401                               => UNAUTHENTICATED
    case 403                               => PERMISSION_DENIED
    case 404                               => NOT_FOUND
    case 429                               => RESOURCE_EXHAUSTED
    case 501                               => UNIMPLEMENTED
    case 503                               => UNAVAILABLE
    case 504                               => DEADLINE_EXCEEDED
    case _                                 => UNKNOWN
  }
}
