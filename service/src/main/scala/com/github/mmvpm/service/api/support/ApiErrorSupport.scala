package com.github.mmvpm.service.api.support

import com.github.mmvpm.service.api.error._
import com.github.mmvpm.service.api.error.CirceInstances._
import com.github.mmvpm.service.api.error.SchemaInstances._
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
        oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequestApiError])),
        oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[UnauthorizedApiError])),
        oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFoundApiError])),
        oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[InternalApiError])),
        oneOfDefaultVariant(jsonBody[InternalApiError])
      )
    )
}
