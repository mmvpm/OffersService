package com.github.mmvpm.util

import cats.data.EitherT
import cats.effect.Sync

import scala.util.Try

object EitherUtils {

  def safe[F[_]: Sync, A](block: => A): EitherT[F, String, A] =
    EitherT(Sync[F].delay(Try(block).toEither)).leftMap(_.getMessage)

  def safeBlocking[F[_]: Sync, A](block: => A): EitherT[F, String, A] =
    EitherT(Sync[F].blocking(Try(block).toEither)).leftMap(_.getMessage)
}
