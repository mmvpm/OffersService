package com.github.mmvpm.parseidon.client.nemia

package object error {

  trait NemiaError {
    def details: String
  }

  case class NemiaApiError(id: String, code: Int, details: String) extends NemiaError

  case class NemiaUnknownError(details: String) extends NemiaError
}
