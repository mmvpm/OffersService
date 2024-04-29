package com.github.mmvpm.moderation.client.ofs

package object error {

  trait OfsClientError {
    def details: String
  }

  case class OfsApiClientError(id: String, code: Int, details: String) extends OfsClientError

  case class OfsUnknownClientError(details: String) extends OfsClientError
}
