package com.github.mmvpm.service.dao.user

import cats.data.EitherT
import cats.effect.kernel.MonadCancelThrow
import cats.implicits.catsSyntaxApplicativeError
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.model.{User, UserID, UserWithPassword}
import com.github.mmvpm.service.dao.error._
import com.github.mmvpm.service.dao.schema.{DoobieSupport, UserPatch, UsersEntry}
import com.github.mmvpm.service.dao.util.ThrowableUtils.DuplicateKeyException
import com.github.mmvpm.util.Logging
import doobie.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragment.Fragment
import doobie.util.invariant.UnexpectedEnd
import doobie.util.transactor.Transactor

class UserDaoPostgresql[F[_]: MonadCancelThrow](implicit val tr: Transactor[F])
    extends UserDao[F]
    with DoobieSupport
    with Logging {

  override def getUser(userId: UserID): EitherT[F, UserDaoError, User] =
    getUserRaw(userId)
      .map(_.toUser)
      .transact(tr)
      .attemptT
      .leftMap {
        case UnexpectedEnd => UserNotFoundDaoError(userId)
        case error         => InternalUserDaoError(error.getMessage)
      }

  override def getUserWithPassword(login: String): EitherT[F, UserDaoError, UserWithPassword] =
    getUserRaw(login)
      .map(_.toUserWithPassword)
      .transact(tr)
      .attemptT
      .leftMap {
        case UnexpectedEnd => UserLoginNotFoundDaoError(login)
        case error         => InternalUserDaoError(error.getMessage)
      }

  override def getUserStatus(userId: UserID): EitherT[F, UserDaoError, UserStatus] =
    getUserStatusRaw(userId)
      .transact(tr)
      .attemptT
      .leftMap {
        case UnexpectedEnd => UserNotFoundDaoError(userId)
        case error         => InternalUserDaoError(error.getMessage)
      }

  override def createUser(user: UserWithPassword): EitherT[F, UserDaoError, Unit] =
    createUserRaw(user)
      .transact(tr)
      .attemptT
      .leftMap {
        case DuplicateKeyException(_) => UserAlreadyExistsDaoError(user.user.login)
        case error => InternalUserDaoError(error.getMessage)
      }
      .flatMap {
        res => EitherT.cond(res, (), InternalUserDaoError(s"$res rows was inserted"))
      }

  override def updateUser(userId: UserID, patch: UserPatch): EitherT[F, UserDaoError, Unit] =
    updateOffersEntryRaw(userId, patch)
      .transact(tr)
      .attemptT
      .handleDefaultErrors

  // queries

  private def getUserRaw(userId: UserID): ConnectionIO[UsersEntry] =
    sql"select id, name, login, status, password_hash, password_salt from users where id = $userId"
      .query[UsersEntry]
      .unique

  private def getUserRaw(login: String): ConnectionIO[UsersEntry] =
    sql"select id, name, login, status, password_hash, password_salt from users where login = $login"
      .query[UsersEntry]
      .unique

  private def getUserStatusRaw(userId: UserID): ConnectionIO[UserStatus] =
    sql"select status from users where id = $userId"
      .query[UserStatus]
      .unique

  private def createUserRaw(user: UserWithPassword): ConnectionIO[Boolean] = {
    import user.password._
    import user.user._
    sql"""
      |insert into users (id, name, login, status, password_hash, password_salt)
      |values ($id, $name, $login, $status, $hash, $salt)
      |""".stripMargin.update.run.map(_ == 1)
  }

  private def updateOffersEntryRaw(userId: UserID, patch: UserPatch): ConnectionIO[Boolean] =
    (fr"update users set " ++ sqlByPatch(patch) ++ fr" where id = $userId")
      .update.run.map(_ == 1)

  // internal

  private def sqlByPatch(patch: UserPatch): Fragment = {
    val nameSql = patch.name.map(name => fr"name = $name")
    val loginSql = patch.login.map(login => fr"login = $login")
    val statusSql = patch.status.map(status => fr"status = $status")
    List(nameSql, loginSql, statusSql).flatten.reduce(_ ++ fr", " ++ _)
  }

  implicit class RichDbUpdate(result: EitherT[F, Throwable, Boolean]) {
    def handleDefaultErrors: EitherT[F, UserDaoError, Unit] =
      result.biflatMap(
        err => EitherT.leftT[F, Unit](InternalUserDaoError(err.getMessage)),
        res => EitherT.cond(res, (), InternalUserDaoError("no rows was updated"))
      )
  }
}
