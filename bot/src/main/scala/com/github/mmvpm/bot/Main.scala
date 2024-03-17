package com.github.mmvpm.bot

import cats.effect.{IO, IOApp}
import com.github.mmvpm.bot.model.MessageID
import com.github.mmvpm.bot.render.RendererImpl
import com.github.mmvpm.bot.state.{State, StateManagerImpl, StorageImpl}
import com.github.mmvpm.bot.util.ResourceUtils
import org.asynchttpclient.Dsl.asyncHttpClient
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

object Main extends IOApp.Simple {

  override def run: IO[Unit] =
    for {
      _ <- IO.println("Starting telegram bot...")

      token = ResourceUtils.readTelegramToken()
      sttpBackend = AsyncHttpClientCatsBackend.usingClient[IO](asyncHttpClient)
      renderer = new RendererImpl
      manager = new StateManagerImpl[IO]
      stateStorage = new StorageImpl[State](State.Started)
      lastMessageStorage = new StorageImpl[Option[MessageID]](None)
      bot = new OfferServiceBot[IO](token, sttpBackend, renderer, manager, stateStorage, lastMessageStorage)

      _ <- bot.startPolling()
    } yield ()
}
