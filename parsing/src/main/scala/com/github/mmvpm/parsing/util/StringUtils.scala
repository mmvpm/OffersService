package com.github.mmvpm.parsing.util

import java.net.{URI, URL}
import scala.util.Try

object StringUtils {

  implicit class StringSyntax(string: String) {

    def toURL: URL = new URI(string).toURL

    def toURLOption: Option[URL] = Try(toURL).toOption
  }
}
