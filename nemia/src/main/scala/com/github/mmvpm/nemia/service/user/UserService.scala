package com.github.mmvpm.nemia.service.user

import cats.data.EitherT
import com.github.mmvpm.nemia.api.request.{RateUserRequest, SignUpRequest, UpdateUserRequest}
import com.github.mmvpm.nemia.api.response.{OkResponse, UserResponse}
import com.github.mmvpm.model.UserID
import com.github.mmvpm.nemia.api.error.ApiError

trait UserService[F[_]] {
  def getUser(userId: UserID): EitherT[F, ApiError, UserResponse]
  def createUser(request: SignUpRequest): EitherT[F, ApiError, UserResponse]
  def updateUser(userId: UserID, request: UpdateUserRequest): EitherT[F, ApiError, UserResponse]
  def deleteUser(userId: UserID): EitherT[F, ApiError, OkResponse]
  def rateUser(fromUserId: UserID, toUserId: UserID, request: RateUserRequest): EitherT[F, ApiError, OkResponse]
}
