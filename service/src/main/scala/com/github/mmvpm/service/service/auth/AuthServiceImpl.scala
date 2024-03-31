package com.github.mmvpm.service.service.auth

import cats.data.EitherT
import cats.effect.std.UUIDGen
import cats.{Functor, Monad}
import com.github.mmvpm.model.{Session, UserID, UserStatus}
import com.github.mmvpm.service.api.error._
import com.github.mmvpm.service.api.response.{SessionResponse, UserIdResponse}
import com.github.mmvpm.service.dao.error._
import com.github.mmvpm.service.dao.session.SessionDao
import com.github.mmvpm.service.dao.user.UserDao
import com.github.mmvpm.service.service.auth.AuthServiceImpl.RichAuthResponse

class AuthServiceImpl[F[_]: Monad: UUIDGen](
    userDao: UserDao[F],
    sessionDao: SessionDao[F]
) extends AuthService[F] {

  override def checkPassword(login: String, password: String): EitherT[F, ApiError, UserID] =
    (for {
      user <- userDao.getUserWithPassword(login)
      _ <- EitherT.cond(user.user.status == UserStatus.Active, (), UserLoginNotFoundDaoError(login): DaoError)
      correctPassword = user.password.check(password)
      id <- EitherT.cond(correctPassword, user.user.id, WrongPasswordDaoError(password): DaoError)
    } yield id).convertError

  override def getSession(userID: UserID): EitherT[F, ApiError, SessionResponse] =
    (for {
      session <- EitherT.liftF(UUIDGen[F].randomUUID)
      _ <- sessionDao.saveSession(userID, session)
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

  import com.github.mmvpm.service.service.user.UserServiceImpl.userConversion

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
