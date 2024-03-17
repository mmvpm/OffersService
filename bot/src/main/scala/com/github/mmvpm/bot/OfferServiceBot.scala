package com.github.mmvpm.bot

import cats.effect.Concurrent
import cats.implicits.toFlatMapOps
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.{Callbacks, Command, Commands}
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.methods.{EditMessageText, SendDice, SendMessage}
import com.bot4s.telegram.models._
import com.github.mmvpm.bot.model.MessageID
import com.github.mmvpm.bot.render.Renderer
import com.github.mmvpm.bot.state.{State, StateManager, Storage}
import com.github.mmvpm.bot.state.State._
import sttp.client3.SttpBackend

class OfferServiceBot[F[_]: Concurrent](
    token: String,
    sttpBackend: SttpBackend[F, Any],
    renderer: Renderer,
    manager: StateManager[F],
    stateStorage: Storage[State],
    lastMessageStorage: Storage[Option[MessageID]])
  extends TelegramBot[F](token, sttpBackend)
  with Polling[F]
  with Commands[F]
  with Callbacks[F] {

  // user sent a message (text, image, etc) to the chat
  onMessage { implicit message =>
    command(message) match {
      case Some(Command("roll", _)) => roll
      case Some(Command("start", _)) => start
      case None =>
        getNextStateTag(stateStorage.get) match {
          case UnknownTag => fail
          case nextTag => replyResolved(nextTag)
        }
    }
  }

  // user pressed the button
  onCallbackQuery { implicit cq =>
    replyResolved(cq.data.get)(cq.message.get)
  }

  // scenarios

  private def roll(implicit message: Message): F[Unit] =
    request(SendDice(message.chat.id)).void

  private def start(implicit message: Message): F[Unit] =
    requestLogged(renderer.render(stateStorage.get, lastMessageStorage.get)).void

  private def replyResolved(tag: String)(implicit message: Message): F[Unit] =
    for {
      nextState <- manager.getNextState(tag, stateStorage.get)
      _ = stateStorage.set(withoutError(nextState))
      reply = renderer.render(nextState, lastMessageStorage.get)
      _ <- requestLogged(reply)
    } yield ()

  private def fail(implicit message: Message): F[Unit] =
    reply("Не понял вас :(").void

  // internal

  private def withoutError(state: State): State =
    state match {
      case Error(returnTo, _) => returnTo
      case _ => state
    }

  private def requestLogged(req: Either[EditMessageText, SendMessage]): F[Unit] =
    req match {
      case Left(toEdit) =>
        for {
          sent <- request(toEdit)
          _ = println(s"Edit $sent")
        } yield ()
      case Right(toSend) =>
        for {
          sent <- request(toSend)
          _ = lastMessageStorage.set(Some(sent.messageId))(sent)
          _ = println(s"Sent $sent")
        } yield ()
    }
}
