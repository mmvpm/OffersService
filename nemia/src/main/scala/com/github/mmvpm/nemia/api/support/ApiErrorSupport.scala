package com.github.mmvpm.nemia.api.support

import com.github.mmvpm.nemia.api.error._
import com.github.mmvpm.nemia.api.error.CirceInstances._
import com.github.mmvpm.nemia.api.error.SchemaInstances._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

trait ApiErrorSupport {

  implicit class RichEndpointWithErrors(endpoint: PublicEndpoint[Unit, Unit, Unit, Any]) {
    def withApiErrors: PublicEndpoint[Unit, ApiError, Unit, Any] = wrapErrors(endpoint)
  }

  private def wrapErrors(endpoint: PublicEndpoint[Unit, Unit, Unit, Any]): PublicEndpoint[Unit, ApiError, Unit, Any] =
    endpoint.errorOut(
      oneOf[ApiError](
        oneOfVariantValueMatcher(StatusCode.BadRequest, jsonBody[BadRequestApiError]) {
          case _: BadRequestApiError => true
        },
        oneOfVariantValueMatcher(StatusCode.Unauthorized, jsonBody[UnauthorizedApiError]) {
          case _: UnauthorizedApiError => true
        },
        oneOfVariantValueMatcher(StatusCode.NotFound, jsonBody[NotFoundApiError]) {
          case _: NotFoundApiError => true
        },
        oneOfVariantValueMatcher(StatusCode.InternalServerError, jsonBody[InternalApiError]) {
          case _: InternalApiError => true
        }
      )
    )
}
