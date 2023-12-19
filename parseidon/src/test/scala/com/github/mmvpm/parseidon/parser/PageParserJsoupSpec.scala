//noinspection TypeAnnotation
package com.github.mmvpm.parseidon.parser

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import com.github.mmvpm.parseidon.model.Page
import com.github.mmvpm.parseidon.util.YoulaTestPageSupport
import com.github.mmvpm.util.StringUtils.RichString
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupDocument
import org.mockito.Mockito.when
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock

class PageParserJsoupSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Fixture with YoulaTestPageSupport {

  "PageParser" - {
    "parse product page correctly" in {
      val page = Page("https://ya.ru".toURL)

      val document = mock[JsoupDocument]
      when(document.toHtml).thenReturn(youlaTestPage)
      when(browser.get(page.url.toString)).thenReturn(document.asInstanceOf[browser.DocumentType])

      parser.parse(page).value.asserting(_ shouldBe Right(List(youlaTestPageItem)))
    }
  }

}

trait Fixture {
  val browser = mock[Browser]
  val parser = new PageParserJsoup[IO](browser)
}
