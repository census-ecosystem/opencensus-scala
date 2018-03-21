package com.github.sebruck.opencensus.akka.http

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import io.opencensus.trace.Status
import io.opencensus.trace.Status._

private[http] object StatusTranslator {

  def translate(httpStatus: StatusCode): Status =
    httpStatus match {
      case status if status.isSuccess()  => Status.OK
      case BadRequest                    => INVALID_ARGUMENT
      case Unauthorized                  => UNAUTHENTICATED
      case Forbidden                     => PERMISSION_DENIED
      case NotFound                      => FAILED_PRECONDITION
      case MethodNotAllowed              => INVALID_ARGUMENT
      case NotAcceptable                 => INVALID_ARGUMENT
      case ProxyAuthenticationRequired   => UNAUTHENTICATED
      case RequestTimeout                => CANCELLED
      case Conflict                      => FAILED_PRECONDITION
      case Gone                          => FAILED_PRECONDITION
      case LengthRequired                => INVALID_ARGUMENT
      case PreconditionFailed            => FAILED_PRECONDITION
      case RequestEntityTooLarge         => INVALID_ARGUMENT
      case RequestUriTooLong             => INVALID_ARGUMENT
      case UnsupportedMediaType          => INVALID_ARGUMENT
      case RequestedRangeNotSatisfiable  => OUT_OF_RANGE
      case ExpectationFailed             => FAILED_PRECONDITION
      case EnhanceYourCalm               => RESOURCE_EXHAUSTED
      case MisdirectedRequest            => FAILED_PRECONDITION
      case UnprocessableEntity           => INVALID_ARGUMENT
      case Locked                        => PERMISSION_DENIED
      case FailedDependency              => FAILED_PRECONDITION
      case PreconditionRequired          => FAILED_PRECONDITION
      case TooManyRequests               => RESOURCE_EXHAUSTED
      case RequestHeaderFieldsTooLarge   => INVALID_ARGUMENT
      case RetryWith                     => UNAVAILABLE
      case UnavailableForLegalReasons    => UNAVAILABLE
      case InternalServerError           => INTERNAL
      case NotImplemented                => UNIMPLEMENTED
      case BadGateway                    => UNAVAILABLE
      case ServiceUnavailable            => UNAVAILABLE
      case GatewayTimeout                => DEADLINE_EXCEEDED
      case HTTPVersionNotSupported       => INVALID_ARGUMENT
      case InsufficientStorage           => RESOURCE_EXHAUSTED
      case BandwidthLimitExceeded        => RESOURCE_EXHAUSTED
      case NotExtended                   => INVALID_ARGUMENT
      case NetworkAuthenticationRequired => UNAUTHENTICATED
      case NetworkReadTimeout            => DEADLINE_EXCEEDED
      case NetworkConnectTimeout         => DEADLINE_EXCEEDED
      case _                             => UNKNOWN
    }
}
