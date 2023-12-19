package com.github.mmvpm.nemia.api.util.request

import cats.effect.IO
import cats.implicits.toBifunctorOps
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.error.ApiError
import com.github.mmvpm.nemia.api.error.CirceInstances._
import com.github.mmvpm.nemia.api.response._
import com.github.mmvpm.nemia.api.util.{ConfigSupport, JsonUtils}
import com.github.mmvpm.nemia.api.util.CirceInstances._
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.client3.circe._

trait UserRequestsSupport extends ConfigSupport {

  def getUser(
      userId: UserID
    )(implicit backend: SttpBackend[IO, Any]): IO[Either[ApiError, UserResponse]] =
    basicRequest
      .post(uri"$baseUrl/api/v1/user/$userId")
      .response(asJsonEither[ApiError, UserResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))
}
