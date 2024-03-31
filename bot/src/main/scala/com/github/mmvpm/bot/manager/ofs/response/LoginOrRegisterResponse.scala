package com.github.mmvpm.bot.manager.ofs.response

sealed trait LoginOrRegisterResponse

object LoginOrRegisterResponse {
  case class LoggedIn(name: String) extends LoginOrRegisterResponse
  case class Registered(password: String) extends LoginOrRegisterResponse
}
