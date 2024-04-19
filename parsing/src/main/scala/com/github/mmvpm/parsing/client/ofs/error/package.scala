package com.github.mmvpm.parsing.client.ofs

package object error {

  trait OfsError {
    def details: String
  }

  case class OfsApiError(id: String, code: Int, details: String) extends OfsError

  case class OfsUnknownError(details: String) extends OfsError
}
