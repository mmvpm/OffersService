package com.github.mmvpm.parseidon.parser

import cats.data.EitherT
import cats.Monad
import cats.effect.Sync
import com.github.mmvpm.parseidon.model.{Page, YoulaItem}
import com.github.mmvpm.parseidon.parser.response.YoulaState
import com.github.mmvpm.util.EitherUtils.safeBlocking
import io.circe.generic.auto._
import io.circe.parser._
import net.ruippeixotog.scalascraper.browser.Browser

class PageParserJsoup[F[_]: Monad: Sync](browser: Browser) extends PageParser[F] {

  import browser.DocumentType

  override def parse(page: Page): EitherT[F, String, List[YoulaItem]] =
    for {
      document <- requestDocument(page)
      json <- getJsonState(document)
      offers <- parseYoulaOffers(json)
    } yield offers

  private def requestDocument(page: Page): EitherT[F, String, DocumentType] =
    safeBlocking(browser.get(page.url.toString))

  private def getJsonState(document: DocumentType): EitherT[F, String, String] =
    for {
      lineWithState <- EitherT.fromOption[F](findYoulaStateLine(document), "__YOULA_STATE__ is not found on the page")
      start = lineWithState.indexWhere(_ == '{')
      end = lineWithState.lastIndexWhere(_ == '}')
      json = lineWithState.slice(start, end + 1)
    } yield json

  private def findYoulaStateLine(document: DocumentType): Option[String] =
    document.toHtml.linesIterator.find { line =>
      line.contains("window.__YOULA_STATE__ = ")
    }

  private def parseYoulaOffers(json: String): EitherT[F, String, List[YoulaItem]] =
    for {
      state <- EitherT.fromEither[F](decode[YoulaState](json)).leftMap(_.getMessage)
      offers = state.toYoulaOffers
      users = state.toYoulaUsers
      items = offers.zip(users).map((YoulaItem.apply _).tupled)
    } yield items

}
