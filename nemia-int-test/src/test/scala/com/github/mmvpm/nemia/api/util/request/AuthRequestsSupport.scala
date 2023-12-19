package com.github.mmvpm.nemia.api.util.request

import cats.effect.IO
import cats.implicits.toBifunctorOps
import com.github.mmvpm.model.UserDescriptionRaw
import com.github.mmvpm.nemia.api.error.ApiError
import com.github.mmvpm.nemia.api.error.CirceInstances._
import com.github.mmvpm.nemia.api.request._
import com.github.mmvpm.nemia.api.response._
import com.github.mmvpm.nemia.api.util.CirceInstances._
import com.github.mmvpm.nemia.ServerConfig
import com.github.mmvpm.nemia.api.util.JsonUtils
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.client3.circe._

trait AuthRequestsSupport {

  def signUp(
      login: String,
      password: String
    )(config: ServerConfig,
      backendStub: SttpBackend[IO, Any]): IO[Either[ApiError, UserResponse]] =
    basicRequest
      .post(uri"${config.host}:${config.port}/api/v1/auth/sign-up")
      .body(SignUpRequest(UserDescriptionRaw(login, password)))
      .response(asJsonEither[ApiError, UserResponse])
      .send(backendStub)
      .map(_.body.leftMap(JsonUtils.parseFailure))
}
