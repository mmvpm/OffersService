package com.github.mmvpm.model

import org.apache.commons.codec.digest.DigestUtils

case class PasswordHashed(hash: String, salt: String) {

  def check(password: String): Boolean =
    DigestUtils.md5Hex(password + salt) == hash
}

object PasswordHashed {

  def make(password: String, salt: String): PasswordHashed =
    PasswordHashed(DigestUtils.md5Hex(password + salt), salt)
}
