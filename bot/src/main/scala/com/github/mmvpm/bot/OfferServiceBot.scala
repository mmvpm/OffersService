package com.github.mmvpm.bot

import cats.effect.Concurrent
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxApplicativeId, toFlatMapOps}
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.{Callbacks, Command, Commands}
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.methods.{Request, SendDice}
import com.bot4s.telegram.models._
import com.github.mmvpm.bot.client.telegram.TelegramClient
import com.github.mmvpm.bot.client.telegram.request.SendMediaGroup
import com.github.mmvpm.bot.manager.ofs.OfsManager
import com.github.mmvpm.bot.manager.ofs.error.OfsError.InvalidSession
import com.github.mmvpm.bot.manager.ofs.response.LoginOrRegisterResponse
import com.github.mmvpm.bot.model.MessageID
import com.github.mmvpm.bot.render.Renderer
import com.github.mmvpm.bot.state.State._
import com.github.mmvpm.bot.state.{State, StateManager, Storage}
import sttp.client3.SttpBackend

class OfferServiceBot[F[_]: Concurrent](
    token: String,
    sttpBackend: SttpBackend[F, Any],
    renderer: Renderer,
    stateManager: StateManager[F],
    stateStorage: Storage[State],
    lastMessageStorage: Storage[Option[MessageID]],
    ofsManager: OfsManager[F],
    telegramClient: TelegramClient[F]
) extends TelegramBot[F](token, sttpBackend)
    with Polling[F]
    with Commands[F]
    with Callbacks[F] {

  // user sent a message (text, image, etc) to the chat
  onMessage { implicit message =>
    (command(message) match {
      case Some(Command("roll", _))  => roll
      case Some(Command("start", _)) => start
      case Some(_)                   => fail
      case None =>
        getNextStateTag(stateStorage.get) match {
          case UnknownTag => fail
          case nextTag    => replyResolved(nextTag)
        }
    }).recover { error =>
      logger.error("Bot failed on message", error)
    }
  }

  // user pressed the button
  onCallbackQuery { implicit cq =>
    replyResolved(cq.data.get)(cq.message.get).recover { error =>
      logger.error("Bot failed on callback query", error)
    }
  }

  // scenarios

  private def roll(implicit message: Message): F[Unit] =
    request(SendDice(message.chat.id)).void

  private def start(implicit message: Message): F[Unit] =
    ofsManager.loginOrRegister.value.flatMap {
      case Left(InvalidSession) =>
        val state: State = EnterPassword
        stateStorage.set(state)
        requestLogged(renderer.render(state, None))
      case Left(error) =>
        reply(s"Ошибка: ${error.details}. Попробуйте команду /start ещё раз").void
      case Right(response) =>
        val state = response match {
          case LoginOrRegisterResponse.LoggedIn(name)       => LoggedIn(name)
          case LoginOrRegisterResponse.Registered(password) => Registered(password)
        }
        val saveMessageId = !state.isInstanceOf[Registered] // to save password in the chat
        requestLogged(renderer.render(state, None), saveMessageId)
    }

  private def replyResolved(tag: String)(implicit message: Message): F[Unit] =
    for {
      nextState <- stateManager.getNextState(tag, stateStorage.get)
      _ = stateStorage.set(withoutError(nextState))
      optPhotosReply = renderer.renderPhotos(nextState)
      reply = renderer.render(nextState, lastMessageStorage.get)
      _ <- optPhotosReply.map(requestLogged(_)).getOrElse(().pure)
      _ <- requestLogged(reply, saveMessageId = optPhotosReply.isEmpty)
    } yield ()

  private def fail(implicit message: Message): F[Unit] =
    reply("Не понял вас :(").void

  // internal

  private def withoutError(state: State): State =
    state match {
      case Error(returnTo, _) => returnTo
      case _                  => state
    }

  private def requestLogged(req: Request[?], saveMessageId: Boolean = true): F[Unit] =
    (req match {
      case my: SendMediaGroup =>
        telegramClient.sendMediaGroup(my)
      case _ =>
        request(req)
    }).map {
      case edited: Either[Boolean, Message] =>
        println(s"Edit $edited")
      case sent: Message =>
        if (saveMessageId) {
          lastMessageStorage.set(Some(sent.messageId))(sent)
        }
        println(s"Sent $sent")
      case messages: Array[Message] =>
//        if (messages.nonEmpty && saveMessageId) {
//          lastMessageStorage.set(Some(messages.head.messageId))(messages.head)
//        }
        println(s"Array ${messages.mkString("\n")}")
      case any =>
        println(s"Any $any")
    }
}
