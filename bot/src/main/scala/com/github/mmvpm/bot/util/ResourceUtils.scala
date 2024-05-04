package com.github.mmvpm.bot.util

import scala.io.Source
import scala.util.Using

object ResourceUtils {

  private def readFile(filename: String): String =
    Using(Source.fromFile(filename))(_.mkString).get

  def readTelegramToken(): String =
    readFile("secret/telegram-token.txt").trim
}
