package com.github.mmvpm.bot

import cats.effect.std.Random
import cats.effect.{ExitCode, IO, IOApp}
import com.github.mmvpm.bot.client.ofs.{OfsClient, OfsClientSttp}
import com.github.mmvpm.bot.client.telegram.{TelegramClient, TelegramClientSttp}
import com.github.mmvpm.bot.manager.ofs.{OfsManager, OfsManagerImpl}
import com.github.mmvpm.bot.model.MessageID
import com.github.mmvpm.bot.render.{Renderer, RendererImpl}
import com.github.mmvpm.bot.state.{State, StateManager, StateManagerImpl, StorageImpl}
import com.github.mmvpm.model.Session
import com.github.mmvpm.secret.{SecretService, SecretServiceImpl}
import com.github.mmvpm.util.ConfigUtils.configByStage
import org.asynchttpclient.Dsl.asyncHttpClient
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      random <- Random.scalaUtilRandom[IO]

      secrets: SecretService[IO] = new SecretServiceImpl[IO]
      token <- secrets.telegramToken.map(_.get)

      config = ConfigSource.resources(configByStage(args)).loadOrThrow[Config]

      sttpBackend = AsyncHttpClientCatsBackend.usingClient[IO](asyncHttpClient)

      stateStorage = new StorageImpl[State](State.Started)
      sessionStorage = new StorageImpl[Option[Session]](None)
      lastMessageStorage = new StorageImpl[Option[MessageID]](None)
      lastPhotosStorage = new StorageImpl[Option[Seq[MessageID]]](None)

      telegramClient: TelegramClient[IO] = new TelegramClientSttp[IO](token, sttpBackend)
      ofsClient: OfsClient[IO] = new OfsClientSttp[IO](config.ofs, sttpBackend)
      ofsManager: OfsManager[IO] = new OfsManagerImpl[IO](ofsClient, sessionStorage, random)

      renderer: Renderer = new RendererImpl
      manager: StateManager[IO] = new StateManagerImpl[IO](ofsManager)

      bot = new OfferServiceBot[IO](
        token,
        sttpBackend,
        renderer,
        manager,
        stateStorage,
        lastMessageStorage,
        lastPhotosStorage,
        ofsManager,
        telegramClient
      )

      _ <- bot.startPolling()

    } yield ExitCode.Success
}
