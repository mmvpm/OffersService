package com.github.mmvpm.util

import java.net.{URI, URL}
import scala.util.Try

object StringUtils {

  implicit class RichString(string: String) {
    def toURL: URL = URI.create(string).toURL
    def tryToURL: Option[URL] = Try(URI.create(string).toURL).toOption
  }
}
