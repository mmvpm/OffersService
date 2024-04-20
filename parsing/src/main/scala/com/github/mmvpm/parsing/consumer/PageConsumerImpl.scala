package com.github.mmvpm.parsing.consumer

import cats.Monad
import cats.data.EitherT
import cats.effect.Temporal
import cats.implicits.{toFunctorOps, toTraverseOps}
import com.github.mmvpm.parsing.YoulaConfig
import com.github.mmvpm.parsing.client.ofs.OfsClient
import com.github.mmvpm.parsing.client.ofs.error._
import com.github.mmvpm.parsing.consumer.PageConsumerImpl.getPassword
import com.github.mmvpm.parsing.dao.queue.PageQueueReader
import com.github.mmvpm.parsing.dao.visited.PageVisitedDao
import com.github.mmvpm.parsing.model.{Page, YoulaItem, YoulaUser}
import com.github.mmvpm.parsing.parser.PageParser
import com.github.mmvpm.util.Logging
import org.apache.commons.codec.digest.DigestUtils
import sttp.model.StatusCode

class PageConsumerImpl[F[_]: Temporal](
    youlaConfig: YoulaConfig,
    pageVisitedDao: PageVisitedDao[F],
    pageQueueReader: PageQueueReader[F],
    pageParser: PageParser[F],
    ofsClient: OfsClient[F]
) extends PageConsumer[F]
    with Logging {

  override def run: EitherT[F, String, Unit] =
    EitherT(Monad[F].iterateWhile(one.value)(_.isRight))

  private def one: EitherT[F, String, Unit] =
    for {
      page <- pageQueueReader.getNextBlocking
      visited <- pageVisitedDao.isVisited(page).recover { error =>
        log.error(s"get visited failed: $error")
        false
      }
      _ = log.info(s"page ${page.url.toString.split('/').last} is already visited: $visited")
      _ <- if (!visited) handlePage(page) else EitherT.pure[F, String](())
    } yield ()

  private def handlePage(page: Page): EitherT[F, String, Unit] =
    for {
      _ <- pageVisitedDao.markVisited(page).recover { error =>
        log.error(s"mark visited failed: $error")
      }
      youlaItems <- pageParser.parse(page).recover { error =>
        log.warn(s"handle page ${page.url} failed: $error")
        List.empty
      }
      _ <- youlaItems.map(handleItem).sequence.void.leftMap(_.details).recover { error =>
        log.error(s"handle item failed: $error")
      }
      _ <- makeDelay
    } yield ()

  private def handleItem(item: YoulaItem): EitherT[F, OfsError, Unit] = {
    val offer = item.offer.toOffer
    val login = item.user.id
    val name = item.user.name
    val password = getPassword(item.user)
    for {
      _ <- ofsClient.signUp(name, login, password).as(()).recover {
        case OfsApiError("user.already.exists", StatusCode.BadRequest.code, _) =>
          // do nothing if the user already exists because the password is known
          ()
        case OfsUnknownError(details) =>
          // consumer will try to work anyway
          log.error(s"ofs sign-up for ${item.user} failed: $details")
          ()
      }
      session <- ofsClient.signIn(login, password).map(_.session)
      createdOffer <- ofsClient.createOffer(session, offer, item.offer.source).map(_.offer)
      _ <- ofsClient.addPhotos(session, createdOffer.id, item.offer.photoUrls)
    } yield ()
  }

  private def makeDelay: EitherT[F, String, Unit] =
    EitherT.liftF(Temporal[F].sleep(youlaConfig.pageRequestDelay))
}

object PageConsumerImpl {

  private val OfsUserPasswordSalt = "password"

  /** To simplify the service, we don't save the conversion "youla-user-id" -> "generated-password". Instead, we get a
    * password by "youla-user-id" using a simple deterministic algorithm.
    */
  private def getPassword(user: YoulaUser): String =
    DigestUtils.md5Hex(user.id + user.name + OfsUserPasswordSalt)
}
