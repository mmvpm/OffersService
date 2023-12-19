package com.github.mmvpm.nemia.service.user

import cats.data.EitherT
import cats.effect.std.{Random, UUIDGen}
import cats.effect.Clock
import cats.{Functor, Monad}
import cats.implicits.toFunctorOps
import cats.implicits.toFlatMapOps
import com.github.mmvpm.nemia.api.request.{RateUserRequest, SignUpRequest, UpdateUserRequest}
import com.github.mmvpm.nemia.api.response.{OkResponse, UserResponse}
import com.github.mmvpm.nemia.dao.user.UserDao
import com.github.mmvpm.nemia.dao.DaoUpdate
import com.github.mmvpm.model.{Mark, PasswordHashed, Rating, User, UserDescription, UserID, UserStatus}
import com.github.mmvpm.nemia.api.error._
import com.github.mmvpm.nemia.dao.error._
import com.github.mmvpm.nemia.service.user.UserServiceImpl._

class UserServiceImpl[F[_]: Monad: Clock: UUIDGen](userDao: UserDao[F], random: Random[F]) extends UserService[F] {

  override def getUser(userId: UserID): EitherT[F, ApiError, UserResponse] =
    userDao.getUser(userId).map(UserResponse.from).convertError

  override def createUser(request: SignUpRequest): EitherT[F, ApiError, UserResponse] =
    (for {
      user <- EitherT.liftF(constructUser(request))
      _ <- userDao.createUser(user)
    } yield UserResponse.from(user)).convertError

  override def updateUser(userId: UserID, request: UpdateUserRequest): EitherT[F, ApiError, UserResponse] =
    (for {
      salt <- EitherT.liftF(random.nextString(PasswordSaltLength))
      user <- userDao.updateUser(userId, updateUserDaoFunc(request, salt))
    } yield UserResponse.from(user)).convertError

  override def deleteUser(userId: UserID): EitherT[F, ApiError, OkResponse] =
    userDao.updateUser(userId, deleteUserDaoFunc).as(OkResponse()).convertError

  override def rateUser(
      fromUserId: UserID,
      toUserId: UserID,
      request: RateUserRequest): EitherT[F, ApiError, OkResponse] =
    (for {
      validMark <- EitherT.pure(1 <= request.mark && request.mark <= 10)
      _ <- EitherT.cond(validMark, (), UserValidationError(s"mark ${request.mark} is not in 1..10"))
      _ <- userDao.updateUser(toUserId, rateUserDaoFunc(fromUserId, request))
    } yield OkResponse()).convertError

  // internal

  private def constructUser(request: SignUpRequest): F[User] =
    for {
      userId <- UUIDGen[F].randomUUID
      salt <- random.nextString(PasswordSaltLength)
      now <- Clock[F].realTimeInstant
      user = User(userId, request.user.encrypt(salt), UserStatus.Active, Rating.empty, now)
    } yield user

  private def updateUserDaoFunc(request: UpdateUserRequest, salt: String)(old: User): DaoUpdate[User] = {
    val password = request.password.map(PasswordHashed.make(_, salt)).getOrElse(old.description.password)
    val email = request.email.orElse(old.description.email)
    val phone = request.phone.orElse(old.description.phone)
    val newUser = old.copy(description = UserDescription(old.description.login, password, email, phone))
    DaoUpdate.SaveNew(newUser)
  }

  private def deleteUserDaoFunc(old: User): DaoUpdate[User] =
    if (old.status != UserStatus.Banned) {
      DaoUpdate.SaveNew(old.copy(status = UserStatus.Deleted))
    } else {
      // user can not 'unban' himself by deleting his profile
      // moreover, 'banned' user is the same as 'deleted' for other users
      DaoUpdate.DoNothing
    }

  private def rateUserDaoFunc(fromUserId: UserID, request: RateUserRequest)(old: User): DaoUpdate[User] = {
    val newMark = Mark(fromUserId, old.id, request.mark)
    val withoutOldMark = old.rating.marks.filter(_.fromId != fromUserId)
    val newMarks = newMark :: withoutOldMark
    val newRating = old.rating.copy(newMarks)
    DaoUpdate.SaveNew(old.copy(rating = newRating))
  }
}

object UserServiceImpl {

  private val PasswordSaltLength = 8

  private[service] implicit class RichUserResponse[F[_]: Functor, T](response: EitherT[F, UserDaoError, T]) {
    def convertError: EitherT[F, ApiError, T] = response.leftMap(userConversion)
  }

  private[service] def userConversion: UserDaoError => ApiError = {
    case UserNotFoundDaoError(userId) => UserNotFoundApiError(userId)
    case UserLoginNotFoundDaoError(login) => UserLoginNotFoundApiError(login)
    case UserAlreadyExistsDaoError(login) => UserAlreadyExistsApiError(login)
    case UserValidationError(details) => UserValidationApiError(details)
    case error => UserDaoInternalApiError(error.details)
  }
}
