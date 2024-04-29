package com.github.mmvpm.moderation.client.util

import cats.effect.kernel.Async
import cats.implicits.catsSyntaxApplicativeId
import com.github.mmvpm.moderation.Config.RetryConfig
import com.github.mmvpm.util.Logging
import retry.RetryDetails.{GivingUp, WillDelayAndRetry}
import retry.RetryPolicies.{fullJitter, limitRetries}
import retry.{RetryDetails, RetryPolicy}
import sttp.client3.SttpClientException

trait RetryUtils[F[_]] {
  def policy: RetryPolicy[F]
  def isSttpClientException(e: Throwable): F[Boolean]
  def onError(error: Throwable, details: RetryDetails): F[Unit]
}

object RetryUtils {

  def impl[F[_]: Async](config: RetryConfig): RetryUtils[F] =
    new Impl[F](config)

  private final class Impl[F[_]: Async](config: RetryConfig) extends RetryUtils[F] with Logging {

    def policy: RetryPolicy[F] =
      limitRetries[F](config.amount).join(fullJitter[F](config.baseDelay))

    def isSttpClientException(e: Throwable): F[Boolean] =
      e match {
        case _: SttpClientException => true.pure[F]
        case _                      => false.pure[F]
      }

    def onError(error: Throwable, details: RetryDetails): F[Unit] =
      Async[F].delay {
        details match {
          case WillDelayAndRetry(_, retries, _) =>
            log.info(s"Sttp client failed: $error. Making retry #${retries + 1}...")
          case GivingUp(totalRetries, _) =>
            log.error(s"Giving up with $error after $totalRetries retries")
        }
      }
  }
}
