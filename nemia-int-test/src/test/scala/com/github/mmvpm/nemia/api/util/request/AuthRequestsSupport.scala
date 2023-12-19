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
import com.github.mmvpm.nemia.api.util.{ConfigSupport, JsonUtils}
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.client3.circe._

trait AuthRequestsSupport extends ConfigSupport {

  def signUp(login: String, password: String)(sttpBackend: SttpBackend[IO, Any]): IO[Either[ApiError, UserResponse]] =
    basicRequest
      .post(uri"$baseUrl/api/v1/auth/sign-up")
      .body(SignUpRequest(UserDescriptionRaw(login, password)))
      .response(asJsonEither[ApiError, UserResponse])
      .send(sttpBackend)
      .map(_.body.leftMap(JsonUtils.parseFailure))
}
