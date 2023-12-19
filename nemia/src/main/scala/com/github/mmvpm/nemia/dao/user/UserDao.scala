package com.github.mmvpm.nemia.dao.user

import cats.data.EitherT
import com.github.mmvpm.nemia.dao.DaoUpdate
import com.github.mmvpm.model.{User, UserID}
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.nemia.dao.error.UserDaoError

trait UserDao[F[_]] {
  def getUser(userId: UserID): EitherT[F, UserDaoError, User]
  def getUser(login: String): EitherT[F, UserDaoError, User]
  def getUserStatus(userId: UserID): EitherT[F, UserDaoError, UserStatus]
  def createUser(user: User): EitherT[F, UserDaoError, Unit]
  def updateUser(userId: UserID, updateFunc: User => DaoUpdate[User]): EitherT[F, UserDaoError, User]
}
