package com.github.mmvpm.model

case class UserDescriptionRaw(
    login: String,
    password: String,
    email: Option[Email] = None,
    phone: Option[Phone] = None
) {

  def encrypt(passwordSalt: String): UserDescription =
    UserDescription(login, PasswordHashed.make(password, passwordSalt), email, phone)
}
