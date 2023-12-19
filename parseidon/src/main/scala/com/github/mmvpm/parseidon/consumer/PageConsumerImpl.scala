package com.github.mmvpm.parseidon.consumer

import cats.data.EitherT
import cats.Monad
import cats.effect.Sync
import cats.implicits.{toFunctorOps, toTraverseOps}
import com.github.mmvpm.parseidon.client.nemia.NemiaClient
import com.github.mmvpm.parseidon.dao.queue.PageQueueReader
import com.github.mmvpm.parseidon.dao.visited.PageVisitedDao
import com.github.mmvpm.parseidon.model.{Page, YoulaItem, YoulaUser}
import com.github.mmvpm.parseidon.parser.PageParser
import com.github.mmvpm.parseidon.YoulaConfig
import com.github.mmvpm.parseidon.client.nemia.error._
import com.github.mmvpm.parseidon.consumer.PageConsumerImpl.getPassword
import com.github.mmvpm.util.EitherUtils.sleep
import com.github.mmvpm.util.Logging
import org.apache.commons.codec.digest.DigestUtils
import sttp.model.StatusCode

class PageConsumerImpl[F[_]: Monad: Sync](
    youlaConfig: YoulaConfig,
    pageVisitedDao: PageVisitedDao[F],
    pageQueueReader: PageQueueReader[F],
    pageParser: PageParser[F],
    nemiaClient: NemiaClient[F]
) extends PageConsumer[F]
    with Logging {

  override def run: EitherT[F, String, Unit] =
    EitherT(Monad[F].iterateWhile(one.value)(_.isRight))

  private def one: EitherT[F, String, Unit] =
    for {
      page <- pageQueueReader.getNextBlocking
      visited <- pageVisitedDao.isVisited(page)
      _ = log.info(s"page ${page.url.toString.split('/').last} is already visited: $visited")
      _ <- if (!visited) handlePage(page) else EitherT.pure[F, String](())
    } yield ()

  private def handlePage(page: Page): EitherT[F, String, Unit] =
    for {
      _ <- pageVisitedDao.markVisited(page)
      youlaItems <- pageParser.parse(page).recover { error =>
        log.warn(s"handle page ${page.url} failed: $error")
        List.empty
      }
      _ <- youlaItems.map(handleItem).sequence.leftMap(_.details)
      _ <- sleep(youlaConfig.pageRequestDelay)
    } yield ()

  private def handleItem(item: YoulaItem): EitherT[F, NemiaError, Unit] = {
    val offer = item.offer.toOffer
    val user = item.user.toUser(getPassword(item.user))
    for { // TODO: retry
      _ <- nemiaClient.signUp(user).as(()).recover {
        case NemiaApiError("user.already.exists", StatusCode.BadRequest.code, _) =>
          // do nothing if the user already exists because the password is known
          ()
        case NemiaUnknownError(details) =>
          // consumer will try to work anyway
          log.error(s"nemia sign-up for ${item.user} failed: $details")
          ()
      }
      signInResponse <- nemiaClient.signIn(user.login, user.password)
      _ <- nemiaClient.createOffer(signInResponse.session, offer)
    } yield ()
  }
}

object PageConsumerImpl {

  private val NemiaUserPasswordSalt = "password"

  /** To simplify the service, we don't save the conversion "youla-user-id" -> "generated-password". Instead, we get a
    * password by "youla-user-id" using a simple deterministic algorithm.
    */
  private def getPassword(user: YoulaUser): String =
    DigestUtils.md5Hex(user.id + user.name + NemiaUserPasswordSalt)
}
