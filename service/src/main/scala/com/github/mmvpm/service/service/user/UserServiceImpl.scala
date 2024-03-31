package com.github.mmvpm.service.service.user

import cats.data.EitherT
import cats.effect.std.{Random, UUIDGen}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import cats.{Functor, Monad}
import com.github.mmvpm.model._
import com.github.mmvpm.service.api.error._
import com.github.mmvpm.service.api.request.SignUpRequest
import com.github.mmvpm.service.api.response.{OkResponse, UserResponse}
import com.github.mmvpm.service.dao.error._
import com.github.mmvpm.service.dao.schema.UserPatch
import com.github.mmvpm.service.dao.user.UserDao
import com.github.mmvpm.service.service.user.UserServiceImpl._

class UserServiceImpl[F[_]: Monad: UUIDGen](userDao: UserDao[F], random: Random[F]) extends UserService[F] {

  override def getUser(userId: UserID): EitherT[F, ApiError, UserResponse] =
    userDao
      .getUser(userId)
      .map(UserResponse)
      .convertError

  override def createUser(request: SignUpRequest): EitherT[F, ApiError, UserResponse] =
    (for {
      user <- EitherT.liftF(constructUser(request))
      _ <- userDao.createUser(user)
    } yield UserResponse(user.user)).convertError

  override def deleteUser(userId: UserID): EitherT[F, ApiError, OkResponse] =
    userDao
      .updateUser(userId, UserPatch(status = Some(UserStatus.Deleted)))
      .flatMap(_ => userDao.getUser(userId))
      .as(OkResponse())
      .convertError

  // internal

  private def constructUser(request: SignUpRequest): F[UserWithPassword] =
    for {
      userId <- UUIDGen[F].randomUUID
      user = User(userId, request.name, request.login, UserStatus.Active)
      salt <- random.nextString(PasswordSaltLength)
      password = PasswordHashed.make(request.password, salt)
      userWithPassword = UserWithPassword(user, password)
    } yield userWithPassword
}

object UserServiceImpl {

  private val PasswordSaltLength = 8

  private[service] implicit class RichUserResponse[F[_]: Functor, T](response: EitherT[F, UserDaoError, T]) {
    def convertError: EitherT[F, ApiError, T] = response.leftMap(userConversion)
  }

  private[service] def userConversion: UserDaoError => ApiError = {
    case UserNotFoundDaoError(userId)     => UserNotFoundApiError(userId)
    case UserLoginNotFoundDaoError(login) => UserLoginNotFoundApiError(login)
    case UserAlreadyExistsDaoError(login) => UserAlreadyExistsApiError(login)
    case WrongPasswordDaoError(password)  => WrongPasswordApiError(password)
    case UserValidationError(details)     => UserValidationApiError(details)
    case error                            => UserDaoInternalApiError(error.details)
  }
}
