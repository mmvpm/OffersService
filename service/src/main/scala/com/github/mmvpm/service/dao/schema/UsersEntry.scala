package com.github.mmvpm.service.dao.schema

import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.model.{PasswordHashed, User, UserID, UserWithPassword}

case class UsersEntry(
    id: UserID,
    name: String,
    login: String,
    status: UserStatus,
    passwordHash: String,
    passwordSalt: String
) {

  def toUserWithPassword: UserWithPassword =
    UserWithPassword(toUser, toPasswordHashed)

  def toUser: User =
    User(id, name, login, status)

  def toPasswordHashed: PasswordHashed =
    PasswordHashed(passwordHash, passwordSalt)
}

object UsersEntry {

  def from(user: UserWithPassword): UsersEntry =
    UsersEntry(
      user.user.id,
      user.user.name,
      user.user.login,
      user.user.status,
      user.password.hash,
      user.password.salt
    )
}
