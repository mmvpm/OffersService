package com.github.mmvpm.nemia.dao.user

import cats.data.EitherT
import cats.effect.kernel.MonadCancelThrow
import cats.free.Free
import cats.implicits.catsSyntaxApplicativeError
import com.github.mmvpm.nemia.dao.{DaoUpdate, QuillSupport}
import com.github.mmvpm.nemia.dao.table.{UserRating, Users}
import com.github.mmvpm.nemia.dao.DaoUpdate._
import com.github.mmvpm.nemia.dao.util.DbSyntax.RichUsers
import com.github.mmvpm.model.{User, UserID, UserStatus}
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.nemia.dao.error._
import com.github.mmvpm.nemia.dao.util.ThrowableUtils.DuplicateKeyException
import com.github.mmvpm.util.Logging
import com.github.mmvpm.util.MonadUtils.{EnsureException, ensure}
import doobie.free.connection.ConnectionOp
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.ConnectionIO
import io.getquill.doobie.DoobieContext
import io.getquill.SnakeCase
import io.getquill.mirrorContextWithQueryProbing.transaction

class UserDaoPostgresql[F[_]: MonadCancelThrow](implicit val tr: Transactor[F])
    extends UserDao[F]
    with QuillSupport
    with Logging {

  private val ctx = new DoobieContext.Postgres(SnakeCase)
  import ctx._

  override def getUser(userId: UserID): EitherT[F, UserDaoError, User] = transaction {
    for {
      users <- run(query[Users].filter(_.id == lift(userId)))
      _ = ensure[F](users.nonEmpty, s"$users.nonEmpty")
      rating <- run(query[UserRating].filter(_.toUserId == lift(userId)))
    } yield assembleUser(users, rating)
  }.transact(tr).attemptT.leftMap {
    case e: EnsureException =>
      UserNotFoundDaoError(userId)
    case t: Throwable =>
      log.error(s"get user $userId failed", t)
      InternalUserDaoError(t.getMessage)
  }

  override def getUser(login: String): EitherT[F, UserDaoError, User] = transaction {
    for {
      users <- run(query[Users].filter(_.login == lift(login)))
      _ = ensure[F](users.nonEmpty, s"$users.nonEmpty")
      rating <- run(query[UserRating].filter(_.toUserId == lift(users.single.id)))
    } yield assembleUser(users, rating)
  }.transact(tr).attemptT.leftMap {
    case e: EnsureException =>
      UserLoginNotFoundDaoError(login)
    case t: Throwable =>
      log.error(s"get user @$login failed", t)
      InternalUserDaoError(t.getMessage)
  }

  override def getUserStatus(userId: UserID): EitherT[F, UserDaoError, UserStatus] =
    for {
      statuses <- getStatus(userId)
      status <- EitherT.fromOption[F](statuses.headOption, UserNotFoundDaoError(userId): UserDaoError)
    } yield status

  override def createUser(user: User): EitherT[F, UserDaoError, Unit] =
    create(Users.from(user), UserRating.from(user)).attemptT.leftMap {
      case e: EnsureException =>
        UserInsertFailedDaoError(user.id)
      case DuplicateKeyException(_) =>
        UserAlreadyExistsDaoError(user.description.login)
      case t: Throwable =>
        log.error(s"create user ${user.id} failed", t)
        InternalUserDaoError(t.getMessage)
    }

  override def updateUser(userId: UserID, updateFunc: User => DaoUpdate[User]): EitherT[F, UserDaoError, User] =
    update(userId, updateFunc).attemptT.leftMap {
      case e: EnsureException =>
        UserUpdateFailedDaoError(userId)
      case t: Throwable =>
        log.error(s"update user $userId failed", t)
        InternalUserDaoError(t.getMessage)
    }

  private def getStatus(userId: UserID): EitherT[F, UserDaoError, List[UserStatus]] = run {
    quote {
      query[Users].filter(_.id == lift(userId)).map(_.status)
    }
  }.transact(tr).attemptT.leftMap { t: Throwable =>
    InternalUserDaoError(t.getMessage)
  }

  private def create(user: Users, userRating: List[UserRating]): F[Unit] = transaction {
    for {
      r1 <- run(query[Users].insertValue(lift(user)))
      r2 <- run(liftQuery(userRating).foreach(query[UserRating].insertValue(_)))
      _ = ensure[F](r1 == 1 && r2.forall(_ == 1), s"$r1 == 1 && $r2.forall(_ == 1)")
    } yield ()
  }.transact(tr)

  private def update(userId: UserID, updateFunc: User => DaoUpdate[User]): F[User] = transaction {
    for {
      dbUser <- run(query[Users].filter(_.id == lift(userId)))
      dbUserRating <- run(query[UserRating].filter(_.toUserId == lift(userId)))
      newUser = assembleUser(dbUser, dbUserRating)
      updateDbUser <- updateFunc(newUser) match {
        case DoNothing        => Free.pure[ConnectionOp, User](newUser)
        case SaveNew(newUser) => updateRaw(userId, newUser)
      }
    } yield updateDbUser
  }.transact(tr)

  private def updateRaw(userId: UserID, newUser: User): ConnectionIO[User] =
    for {
      r1 <- run {
        query[Users]
          .filter(_.id == lift(userId))
          .updateValue(lift(Users.from(newUser)))
      }
      r2 <- run {
        query[UserRating]
          .filter(_.toUserId == lift(userId))
          .delete
      }
      r3 <- run {
        liftQuery(UserRating.from(newUser)).foreach { rating =>
          query[UserRating].insertValue(rating)
        }
      }
      _ = ensure[F](r1 == 1 && r2 == 1 && r3.forall(_ == 1), s"$r1 == 1 && $r2 == 1 && $r3.forall(_ == 1)")
    } yield newUser

  private def assembleUser(dbUsers: List[Users], dbRating: List[UserRating]): User =
    UserRating.mergeTo(dbUsers.single.toUser, dbRating)
}
