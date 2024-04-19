package com.github.mmvpm.parsing.client.util

import cats.effect.kernel.Async
import cats.implicits.catsSyntaxApplicativeId
import com.github.mmvpm.parsing.RetryConfiguration
import com.github.mmvpm.util.Logging
import retry.{RetryDetails, RetryPolicy}
import retry.RetryDetails.{GivingUp, WillDelayAndRetry}
import retry.RetryPolicies.{fullJitter, limitRetries}
import sttp.client3.SttpClientException

trait RetryUtils[F[_]] {
  def policy: RetryPolicy[F]
  def isSttpClientException(e: Throwable): F[Boolean]
  def onError(error: Throwable, details: RetryDetails): F[Unit]
}

class RetryUtilsImpl[F[_]: Async](config: RetryConfiguration) extends RetryUtils[F] with Logging {

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
