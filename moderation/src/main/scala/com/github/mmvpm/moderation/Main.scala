package com.github.mmvpm.moderation

import cats.effect.{ExitCode, IO, IOApp}
import com.github.mmvpm.moderation.client.ofs.{OfsClient, OfsClientRetrying}
import com.github.mmvpm.moderation.client.util.RetryUtils
import com.github.mmvpm.moderation.service.ModerationService
import com.github.mmvpm.moderation.worker.ModerationWorker
import com.github.mmvpm.util.Logging
import org.asynchttpclient.Dsl.asyncHttpClient
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

object Main extends IOApp with Logging {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- IO.println("Moderation worker started")

      config = ConfigSource.default.loadOrThrow[Config]

      sttpBackend = AsyncHttpClientCatsBackend.usingClient[IO](asyncHttpClient)
      retryUtils = RetryUtils.impl[IO](config.retry)

      ofsClient = OfsClient.sttp[IO](config.ofs, sttpBackend)
      ofsClientRetrying = new OfsClientRetrying(ofsClient, retryUtils)
      moderationService = ModerationService.impl[IO]
      moderationWorker = ModerationWorker.impl[IO](ofsClientRetrying, moderationService, config.worker)

      _ <- moderationWorker.run.recover { error =>
        log.error(s"Moderation worker failed", error)
      }
    } yield ExitCode.Success
}
