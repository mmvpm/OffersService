package com.github.mmvpm.nemia.dao.table

import com.github.mmvpm.model._
import com.github.mmvpm.model.UserStatus.UserStatus

import java.time.Instant

case class Users(
    id: UserID,
    login: String,
    passwordHash: String,
    passwordSalt: String,
    email: Option[Email],
    phone: Option[Phone],
    status: UserStatus,
    registeredAt: Instant
) {

  def toUser: User =
    User(
      id,
      UserDescription(login, PasswordHashed(passwordHash, passwordSalt), email, phone),
      status,
      Rating.empty,
      registeredAt
    )
}

object Users {

  def from(user: User): Users =
    Users(
      user.id,
      user.description.login,
      user.description.password.hash,
      user.description.password.salt,
      user.description.email,
      user.description.phone,
      user.status,
      user.registeredAt
    )
}
