package com.github.mmvpm.model

case class UserDescription(
    login: String,
    password: PasswordHashed,
    email: Option[Email] = None,
    phone: Option[Phone] = None
)
