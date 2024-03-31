package com.github.mmvpm.service.service.user

import cats.data.EitherT
import com.github.mmvpm.model.UserID
import com.github.mmvpm.service.api.error.ApiError
import com.github.mmvpm.service.api.request.SignUpRequest
import com.github.mmvpm.service.api.response.{OkResponse, UserResponse}

trait UserService[F[_]] {
  def getUser(userId: UserID): EitherT[F, ApiError, UserResponse]
  def createUser(request: SignUpRequest): EitherT[F, ApiError, UserResponse]
  def deleteUser(userId: UserID): EitherT[F, ApiError, OkResponse]
}
