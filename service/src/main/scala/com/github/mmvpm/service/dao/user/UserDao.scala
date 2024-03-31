package com.github.mmvpm.service.dao.user

import cats.data.EitherT
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.model.{User, UserID, UserWithPassword}
import com.github.mmvpm.service.dao.error.UserDaoError
import com.github.mmvpm.service.dao.schema.UserPatch

trait UserDao[F[_]] {
  def getUser(userId: UserID): EitherT[F, UserDaoError, User]
  def getUserWithPassword(login: String): EitherT[F, UserDaoError, UserWithPassword]
  def getUserStatus(userId: UserID): EitherT[F, UserDaoError, UserStatus]
  def createUser(user: UserWithPassword): EitherT[F, UserDaoError, Unit]
  def updateUser(userId: UserID, patch: UserPatch): EitherT[F, UserDaoError, Unit]
}
