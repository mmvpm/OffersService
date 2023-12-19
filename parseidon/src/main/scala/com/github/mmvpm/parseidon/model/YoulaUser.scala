package com.github.mmvpm.parseidon.model

import com.github.mmvpm.model.UserDescriptionRaw

case class YoulaUser(id: String, name: String) {

  def toUser(password: String): UserDescriptionRaw =
    UserDescriptionRaw(login, password)

  private def login: String =
    s"$name ($id)".take(64) // nemia restriction: max login size is 64
}
