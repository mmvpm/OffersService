package com.github.mmvpm.parsing.dao.util

import com.github.mmvpm.parsing.model.Page
import com.github.mmvpm.parsing.util.StringUtils.StringSyntax

object PageSyntax {

  implicit class RichPage(page: Page) {
    def toRedis: String = page.url.toString
  }

  implicit class RichString(string: String) {
    def fromRedis: Page = Page(string.toURL)
  }
}
