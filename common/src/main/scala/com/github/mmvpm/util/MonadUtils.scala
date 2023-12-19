package com.github.mmvpm.util

import cats.effect.MonadCancelThrow

object MonadUtils {

  class EnsureException(message: String) extends RuntimeException(message)

  def ensure[F[_]: MonadCancelThrow](condition: Boolean, comment: String): F[Unit] =
    MonadCancelThrow[F].raiseWhen(condition)(new EnsureException(comment))
}
