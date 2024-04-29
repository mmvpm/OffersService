package com.github.mmvpm.moderation.service

import cats.Applicative
import cats.implicits._
import com.github.mmvpm.model.Offer
import com.github.mmvpm.moderation.model.Resolution

trait ModerationService[F[_]] {
  def check(offer: Offer): F[Resolution]
}

object ModerationService {

  def impl[F[_]: Applicative]: ModerationService[F] =
    new Impl[F]

  private final class Impl[F[_]: Applicative] extends ModerationService[F] {
    def check(offer: Offer): F[Resolution] = {
      val text = s"${offer.description.name} ${offer.description.text}"
      val shouldBan = text.contains("дуб")
      val resolution: Resolution = if (shouldBan) Resolution.Ban else Resolution.Ok
      resolution.pure[F]
    }
  }
}
