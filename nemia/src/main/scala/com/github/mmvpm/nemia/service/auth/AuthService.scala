package com.github.mmvpm.nemia.service.auth

import cats.data.EitherT
import com.github.mmvpm.nemia.api.response.{SessionResponse, UserIdResponse}
import com.github.mmvpm.model.{Session, UserID}
import com.github.mmvpm.nemia.api.error.ApiError

trait AuthService[F[_]] {
  def checkPassword(login: String, password: String): EitherT[F, ApiError, UserID]
  def getSession(userID: UserID): EitherT[F, ApiError, SessionResponse]
  def resolveSession(session: Session): EitherT[F, ApiError, UserID]
  def whoami(session: Session): EitherT[F, ApiError, UserIdResponse]
}
