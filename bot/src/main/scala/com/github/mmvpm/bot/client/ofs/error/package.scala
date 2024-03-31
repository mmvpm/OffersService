package com.github.mmvpm.bot.client.ofs

package object error {

  trait OfsClientError {
    def details: String
  }

  case class OfsApiClientError(id: String, code: Int, details: String) extends OfsClientError

  case class OfsUnknownClientError(details: String) extends OfsClientError
}
