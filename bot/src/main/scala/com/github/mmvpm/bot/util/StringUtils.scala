package com.github.mmvpm.bot.util

import java.util.UUID
import scala.util.Try

object StringUtils {

  implicit class RichString(string: String) {

    def isUUID: Boolean =
      Try(UUID.fromString(string)).isSuccess

    def toUUID: UUID =
      UUID.fromString(string)
  }
}
