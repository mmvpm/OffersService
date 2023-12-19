package com.github.mmvpm.nemia.api.response

import com.github.mmvpm.model._
import com.github.mmvpm.model.UserStatus.UserStatus

import java.time.Instant

case class UserResponse(user: ApiUser)

object UserResponse {

  def from(user: User): UserResponse =
    UserResponse(ApiUser.from(user))
}

case class ApiUser(
    id: UserID,
    description: ApiUserDescription,
    status: UserStatus,
    rating: Rating,
    registeredAt: Instant)

object ApiUser {

  def from(user: User): ApiUser =
    ApiUser(user.id, ApiUserDescription.from(user.description), user.status, user.rating, user.registeredAt)
}

case class ApiUserDescription(
    login: String,
    email: Option[Email] = None,
    phone: Option[Phone] = None)

object ApiUserDescription {

  def from(description: UserDescription): ApiUserDescription =
    ApiUserDescription(description.login, description.email, description.phone)
}
