package com.github.sebruck.opencensus.http

import io.opencensus.trace.Status
import io.opencensus.trace.Status._

object StatusTranslator {

  def translate(httpStatus: Int): Status = httpStatus match {
    case 200 => OK
    case 400 => INVALID_ARGUMENT
    case 401 => UNAUTHENTICATED
    case 403 => PERMISSION_DENIED
    case 404 => FAILED_PRECONDITION
    case 405 => INVALID_ARGUMENT
    case 406 => INVALID_ARGUMENT
    case 407 => UNAUTHENTICATED
    case 408 => CANCELLED
    case 409 => FAILED_PRECONDITION
    case 410 => FAILED_PRECONDITION
    case 411 => INVALID_ARGUMENT
    case 412 => FAILED_PRECONDITION
    case 413 => INVALID_ARGUMENT
    case 414 => INVALID_ARGUMENT
    case 415 => INVALID_ARGUMENT
    case 416 => OUT_OF_RANGE
    case 417 => FAILED_PRECONDITION
    case 420 => RESOURCE_EXHAUSTED
    case 421 => FAILED_PRECONDITION
    case 422 => INVALID_ARGUMENT
    case 423 => PERMISSION_DENIED
    case 424 => FAILED_PRECONDITION
    case 428 => FAILED_PRECONDITION
    case 429 => RESOURCE_EXHAUSTED
    case 431 => INVALID_ARGUMENT
    case 449 => UNAVAILABLE
    case 451 => UNAVAILABLE

    case 500 => INTERNAL
    case 501 => UNIMPLEMENTED
    case 502 => UNAVAILABLE
    case 503 => UNAVAILABLE
    case 504 => DEADLINE_EXCEEDED
    case 505 => INVALID_ARGUMENT
    case 507 => RESOURCE_EXHAUSTED
    case 509 => RESOURCE_EXHAUSTED
    case 510 => INVALID_ARGUMENT
    case 511 => UNAUTHENTICATED
    case 598 => DEADLINE_EXCEEDED
    case 599 => DEADLINE_EXCEEDED
    case _   => UNKNOWN
  }
}
