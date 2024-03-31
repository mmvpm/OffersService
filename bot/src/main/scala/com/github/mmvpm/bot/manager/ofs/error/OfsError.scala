package com.github.mmvpm.bot.manager.ofs.error

sealed trait OfsError {
  def details: String
}

object OfsError {

  case object InvalidSession extends OfsError {
    override def details: String = "Session is invalid or expired"
  }

  case class OfsSomeError(details: String) extends OfsError
}
