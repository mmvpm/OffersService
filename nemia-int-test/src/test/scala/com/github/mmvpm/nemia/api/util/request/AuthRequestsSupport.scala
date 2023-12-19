package com.github.mmvpm.nemia.api.util.request

import cats.effect.IO
import cats.implicits.toBifunctorOps
import com.github.mmvpm.model.{Session, UserDescriptionRaw}
import com.github.mmvpm.nemia.api.error.ApiError
import com.github.mmvpm.nemia.api.error.CirceInstances._
import com.github.mmvpm.nemia.api.request._
import com.github.mmvpm.nemia.api.response._
import com.github.mmvpm.nemia.api.util.{ConfigSupport, JsonUtils}
import com.github.mmvpm.nemia.api.util.CirceInstances._
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.client3.circe._

trait AuthRequestsSupport extends ConfigSupport {

  def signUp(login: String, password: String)(backend: SttpBackend[IO, Any]): IO[Either[ApiError, UserResponse]] =
    basicRequest
      .post(uri"$baseUrl/api/v1/auth/sign-up")
      .body(SignUpRequest(UserDescriptionRaw(login, password)))
      .response(asJsonEither[ApiError, UserResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))

  def signIn(login: String, password: String)(backend: SttpBackend[IO, Any]): IO[Either[ApiError, SessionResponse]] =
    basicRequest
      .post(uri"$baseUrl/api/v1/auth/sign-in")
      .auth.basic(login, password)
      .response(asJsonEither[ApiError, SessionResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))

  def whoami(session: Session)(backend: SttpBackend[IO, Any]): IO[Either[ApiError, UserIdResponse]] =
    basicRequest
      .get(uri"$baseUrl/api/v1/auth/whoami?$session")
      .response(asJsonEither[ApiError, UserIdResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))
}
