package com.github.mmvpm.nemia.api.request

import com.github.mmvpm.model.{Email, Phone}

case class UpdateUserRequest(password: Option[String] = None, email: Option[Email] = None, phone: Option[Phone] = None)
