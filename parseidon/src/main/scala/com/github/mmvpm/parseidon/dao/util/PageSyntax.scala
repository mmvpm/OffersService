package com.github.mmvpm.parseidon.dao.util

import com.github.mmvpm.parseidon.model.Page
import com.github.mmvpm.util.StringUtils.{RichString => RichStringURL}

object PageSyntax {

  implicit class RichPage(page: Page) {
    def toRedis: String = page.url.toString
  }

  implicit class RichString(string: String) {
    def fromRedis: Page = Page(string.toURL)
  }
}
