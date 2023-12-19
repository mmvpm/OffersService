package com.github.mmvpm.nemia.api.util.request

import cats.effect.IO
import cats.implicits.toBifunctorOps
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.error.ApiError
import com.github.mmvpm.nemia.api.error.CirceInstances._
import com.github.mmvpm.nemia.api.request.{RateUserRequest, UpdateUserRequest}
import com.github.mmvpm.nemia.api.response._
import com.github.mmvpm.nemia.api.util.{ConfigSupport, JsonUtils}
import com.github.mmvpm.nemia.api.util.CirceInstances._
import com.github.mmvpm.nemia.api.SessionHeaderName
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.client3.circe._

trait UserRequestsSupport extends ConfigSupport {

  def getUser(
      userId: UserID
    )(implicit backend: SttpBackend[IO, Any]): IO[Either[ApiError, UserResponse]] =
    basicRequest
      .get(uri"$baseUrl/api/v1/user/$userId")
      .response(asJsonEither[ApiError, UserResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))

  def updateUser(
      session: Session,
      request: UpdateUserRequest
    )(implicit backend: SttpBackend[IO, Any]): IO[Either[ApiError, UserResponse]] =
    basicRequest
      .put(uri"$baseUrl/api/v1/user")
      .header(SessionHeaderName, session.toString)
      .body(request)
      .response(asJsonEither[ApiError, UserResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))

  def deleteUser(
      session: Session
    )(implicit backend: SttpBackend[IO, Any]): IO[Either[ApiError, OkResponse]] =
    basicRequest
      .delete(uri"$baseUrl/api/v1/user")
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[ApiError, OkResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))

  def rateUser(
      session: Session,
      toUserId: UserID,
      mark: Int
    )(implicit backend: SttpBackend[IO, Any]): IO[Either[ApiError, OkResponse]] =
    basicRequest
      .put(uri"$baseUrl/api/v1/user/rate/$toUserId")
      .header(SessionHeaderName, session.toString)
      .body(RateUserRequest(mark))
      .response(asJsonEither[ApiError, OkResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))
}
