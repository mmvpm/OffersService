package com.github.mmvpm.nemia.dao.user

import cats.data.EitherT
import cats.Monad
import cats.effect.kernel.Sync
import com.github.mmvpm.nemia.dao.DaoUpdate
import com.github.mmvpm.model.{User, UserID, UserStatus}
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.nemia.dao.error._

import scala.collection.mutable

class UserDaoInMemory[F[_]: Monad: Sync] extends UserDao[F] {

  override def getUser(userId: UserID): EitherT[F, UserDaoError, User] =
    EitherT.fromOption(storageUsers.find(_.id == userId), UserNotFoundDaoError(userId))

  override def getUser(login: String): EitherT[F, UserDaoError, User] =
    EitherT.fromOption(storageUsers.find(_.description.login == login), UserLoginNotFoundDaoError(login))

  override def getUserStatus(userId: UserID): EitherT[F, UserDaoError, UserStatus] =
    getUser(userId).map(_.status)

  override def createUser(user: User): EitherT[F, UserDaoError, Unit] =
    for {
      _ <- EitherT.cond(isNewUserUnique(user), (), UserAlreadyExistsDaoError(user.description.login))
      _ <- EitherT.liftF(Sync[F].delay(storageUsers.append(user)))
    } yield ()

  override def updateUser(userId: UserID, updateFunc: User => DaoUpdate[User]): EitherT[F, UserDaoError, User] =
    for {
      user <- EitherT.fromOption(storageUsers.find(_.id == userId), UserNotFoundDaoError(userId))
      newUser <- EitherT.liftF {
        updateFunc(user) match {
          case DaoUpdate.DoNothing => Monad[F].pure(user)
          case DaoUpdate.SaveNew(newUser) => Sync[F].delay {
            storageUsers.update(storageUsers.indexOf(user), newUser)
            newUser
          }
        }
      }
    } yield newUser

  // internal

  private def isNewUserUnique(newUser: User): Boolean =
    !storageUsers.exists { current =>
      current.id == newUser.id || current.description.login == newUser.description.login
    }

  private val storageUsers: mutable.Buffer[User] = new mutable.ArrayBuffer()
}
