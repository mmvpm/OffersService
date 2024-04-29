package com.github.mmvpm.moderation.worker

import cats.data.EitherT
import cats.effect.Temporal
import cats.implicits._
import com.github.mmvpm.model.{Offer, OfferStatus}
import com.github.mmvpm.moderation.Config.WorkerConfig
import com.github.mmvpm.moderation.client.ofs.OfsClient
import com.github.mmvpm.moderation.model.Resolution
import com.github.mmvpm.moderation.service.ModerationService
import com.github.mmvpm.util.Logging

trait ModerationWorker[F[_]] {
  def run: F[Unit]
}

object ModerationWorker extends Logging {

  private val BatchSize = 1000

  def impl[F[_]: Temporal](
      ofsClient: OfsClient[F],
      moderationService: ModerationService[F],
      config: WorkerConfig
  ): ModerationWorker[F] =
    new Impl[F](ofsClient, moderationService, config)

  private final class Impl[F[_]: Temporal](
      ofsClient: OfsClient[F],
      moderationService: ModerationService[F],
      config: WorkerConfig
  ) extends ModerationWorker[F] {

    def run: F[Unit] =
      one.foreverM

    private def one: F[Unit] =
      for {
        offers <- ofsClient.getOffersByStatus(OfferStatus.OnModeration, BatchSize).value.map {
          case Left(error) =>
            log.error(s"Get offers with OnModeration status failed: $error")
            List.empty
          case Right(response) =>
            response.offers
        }
        _ = log.info(s"Checking ${offers.size} offers")
        resolutions <- offers.traverse(moderationService.check)
        newStatuses = offers.zip(resolutions).collect {
          case (offer, Resolution.Ok)  => (offer.id, OfferStatus.Active)
          case (offer, Resolution.Ban) => (offer.id, OfferStatus.Banned)
        }
        _ <- ofsClient.updateOfferStatusBatch(newStatuses).value.map {
          case Left(error) => log.error(s"Update offer statuses failed: $error")
          case Right(_)    => ()
        }
        _ <- makeDelay(offers)
      } yield ()

    private def makeDelay(offers: Seq[Offer]): F[Unit] =
      if (offers.isEmpty)
        Temporal[F].sleep(config.delayWait)
      else
        Temporal[F].sleep(config.delayWork)
  }
}
