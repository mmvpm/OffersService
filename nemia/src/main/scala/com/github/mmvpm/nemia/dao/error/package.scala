package com.github.mmvpm.nemia.dao

import com.github.mmvpm.model.{OfferID, Session, UserID}

package object error {

  sealed trait DaoError {
    def details: String
  }

  // session

  sealed trait SessionDaoError extends DaoError

  case class SessionNotFoundDaoError(session: Session) extends SessionDaoError {
    val details: String = s"session '$session' is not found"
  }

  case class SessionGetFailedDaoError(session: Session) extends SessionDaoError {
    val details: String = s"failed to get user id by session'$session'"
  }

  case class SessionSaveFailedDaoError(session: Session) extends SessionDaoError {
    val details: String = s"failed to save session '$session'"
  }

  case class InvalidRedisValueDaoError(value: String) extends SessionDaoError {
    val details: String = s"redis value is not valid uuid: '$value'"
  }

  // user

  sealed trait UserDaoError extends DaoError

  case class UserNotFoundDaoError(userId: UserID) extends UserDaoError {
    val details: String = s"user $userId is not found"
  }

  case class UserLoginNotFoundDaoError(login: String) extends UserDaoError {
    val details: String = s"user @$login is not found"
  }

  case class WrongPasswordDaoError(password: String) extends UserDaoError {
    val details: String = s"incorrect password '$password''"
  }

  case class UserAlreadyExistsDaoError(login: String) extends UserDaoError {
    val details: String = s"user @$login already exists"
  }

  case class UserValidationError(details: String) extends UserDaoError

  case class UserInsertFailedDaoError(userId: UserID) extends UserDaoError {
    val details: String = s"failed to insert user $userId"
  }

  case class UserUpdateFailedDaoError(userId: UserID) extends UserDaoError {
    val details: String = s"failed to insert user $userId"
  }

  case class InternalUserDaoError(message: String) extends UserDaoError {
    val details: String = s"database internal error: $message"
  }

  // offer

  sealed trait OfferDaoError extends DaoError

  case class OfferNotFoundDaoError(offerId: OfferID) extends OfferDaoError {
    val details: String = s"offer $offerId is not found"
  }

  case class OfferAlreadyExistsDaoError(offerId: OfferID) extends OfferDaoError {
    val details: String = s"offer $offerId already exists"
  }

  case class OfferInsertFailedDaoError(offerId: OfferID) extends OfferDaoError {
    val details: String = s"failed to insert offer $offerId"
  }

  case class OfferUpdateFailedDaoError(offerId: OfferID) extends OfferDaoError {
    val details: String = s"failed to update offer $offerId"
  }

  case class OfferBelongingFailedDaoError(offerId: OfferID, userId: UserID) extends OfferDaoError {
    val details: String = s"offer $offerId does not belong to user $userId"
  }

  case class InternalOfferDaoError(message: String = "") extends OfferDaoError {
    val details: String = s"database internal error" + suffix(message)
  }

  // utils

  private def suffix(message: String): String =
    if (message.isEmpty) "" else s": $message"
}
