package com.github.mmvpm.nemia.service.auth

import cats.data.EitherT
import cats.{Functor, Monad}
import cats.effect.std.UUIDGen
import cats.effect.Clock
import cats.implicits.toBifunctorOps
import com.github.mmvpm.nemia.api.response.{SessionResponse, UserIdResponse}
import com.github.mmvpm.nemia.dao.session.SessionDao
import com.github.mmvpm.nemia.dao.user.UserDao
import com.github.mmvpm.model.{Session, UserID, UserStatus}
import com.github.mmvpm.nemia.api.error._
import com.github.mmvpm.nemia.dao.error._
import com.github.mmvpm.nemia.service.auth.AuthServiceImpl.RichAuthResponse
import sttp.model.headers.CookieValueWithMeta

import java.time.Instant

class AuthServiceImpl[F[_]: Monad: Clock: UUIDGen](
    userDao: UserDao[F],
    sessionDao: SessionDao[F],
    SessionExpirationSeconds: Long
) extends AuthService[F] {

  override def checkPassword(login: String, password: String): EitherT[F, ApiError, UserID] =
    (for {
      user <- userDao.getUser(login)
      _ <- EitherT.cond(user.status == UserStatus.Active, (), UserLoginNotFoundDaoError(login): DaoError)
      correctPassword <- EitherT.pure(user.description.password.check(password))
      id <- EitherT.cond(correctPassword, user.id, WrongPasswordDaoError(password): DaoError)
    } yield id).convertError

  override def getSession(userID: UserID): EitherT[F, ApiError, SessionResponse] =
    (for {
      session <- EitherT.liftF(UUIDGen[F].randomUUID)
      _ <- sessionDao.saveSession(userID, session)
//      now <- EitherT.liftF(Clock[F].realTimeInstant)
//      cookie <- EitherT.fromEither[F]( // header is used instead of cookie now
//        CookieValueWithMeta.safeApply(
//          session.toString,
//          expires = Some(now.plusSeconds(SessionExpirationSeconds)),
//          secure = true,
//          httpOnly = true
//        ).leftMap(_ => SessionNotFoundDaoError(session): SessionDaoError)
//      )
    } yield SessionResponse(session)).convertError

  override def resolveSession(session: Session): EitherT[F, ApiError, UserID] =
    (for {
      userId <- sessionDao.getUserId(session)
      userStatus <- userDao.getUserStatus(userId)
      _ <- EitherT.cond(userStatus == UserStatus.Active, (), UserNotFoundDaoError(userId): DaoError)
    } yield userId).convertError

  override def whoami(session: Session): EitherT[F, ApiError, UserIdResponse] =
    resolveSession(session).map(UserIdResponse)
}

object AuthServiceImpl {

  import com.github.mmvpm.nemia.service.user.UserServiceImpl.userConversion

  private[service] implicit class RichAuthResponse[F[_]: Functor, T, Error <: DaoError](
      response: EitherT[F, Error, T]
  ) {
    def convertError: EitherT[F, ApiError, T] =
      response.leftMap {
        case e: SessionDaoError => sessionConversion(e)
        case e: UserDaoError    => userConversion(e)
      }
  }

  private[service] def sessionConversion: SessionDaoError => ApiError = {
    case SessionNotFoundDaoError(session) => InvalidSessionApiError(session)
    case error                            => SessionDaoInternalApiError(error.details)
  }
}
