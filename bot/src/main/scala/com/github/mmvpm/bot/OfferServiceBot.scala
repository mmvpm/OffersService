package com.github.mmvpm.bot

import cats.effect.Concurrent
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toTraverseOps}
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.{Callbacks, Command, Commands}
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.methods.{DeleteMessage, Request, SendDice}
import com.bot4s.telegram.models._
import com.github.mmvpm.bot.client.telegram.TelegramClient
import com.github.mmvpm.bot.client.telegram.request.{EditMessageMedia, SendMediaGroup}
import com.github.mmvpm.bot.manager.ofs.OfsManager
import com.github.mmvpm.bot.manager.ofs.error.OfsError.InvalidSession
import com.github.mmvpm.bot.manager.ofs.response.LoginOrRegisterResponse
import com.github.mmvpm.bot.model.{ChatID, MessageID}
import com.github.mmvpm.bot.render.Renderer
import com.github.mmvpm.bot.state.State._
import com.github.mmvpm.bot.state.{State, StateManager, Storage}
import sttp.client3.SttpBackend

import scala.util.Try

class OfferServiceBot[F[_]: Concurrent](
    token: String,
    sttpBackend: SttpBackend[F, Any],
    renderer: Renderer,
    stateManager: StateManager[F],
    stateStorage: Storage[State],
    lastMessageStorage: Storage[Option[MessageID]],
    lastPhotosStorage: Storage[Option[Seq[MessageID]]],
    ofsManager: OfsManager[F],
    telegramClient: TelegramClient[F]
) extends TelegramBot[F](token, sttpBackend)
    with Polling[F]
    with Commands[F]
    with Callbacks[F] {

  // user sent a message (text, image, etc) to the chat
  onMessage { implicit message =>
    safe(
      command(message) match {
        case Some(Command("roll", _))  => roll
        case Some(Command("start", _)) => start
        case Some(_)                   => fail
        case None =>
          getNextStateTag(stateStorage.get) match {
            case UnknownTag => fail
            case nextTag    => replyResolved(nextTag)
          }
      },
      ().pure
    )
  }

  // user pressed the button
  onCallbackQuery { implicit cq =>
    safe(replyResolved(cq.data.get)(cq.message.get), ().pure)
  }

  // scenarios

  private def roll(implicit message: Message): F[Unit] =
    request(SendDice(message.chat.id)).void

  private def start(implicit message: Message): F[Unit] =
    ofsManager.loginOrRegister.value.flatMap {
      case Left(InvalidSession) =>
        val state: State = EnterPassword
        stateStorage.set(state)
        requestLogged(renderer.render(state, None, None))
      case Left(error) =>
        reply(s"Ошибка: ${error.details}. Попробуйте команду /start ещё раз").void
      case Right(response) =>
        val state = response match {
          case LoginOrRegisterResponse.LoggedIn(name)       => LoggedIn(name)
          case LoginOrRegisterResponse.Registered(password) => Registered(password)
        }
        val saveMessageId = !state.isInstanceOf[Registered] // to save password in the chat
        requestLogged(renderer.render(state, None, None), saveMessageId)
    }

  private def replyResolved(tag: String)(implicit message: Message): F[Unit] =
    for {
      nextState <- stateManager.getNextState(tag, stateStorage.get)
      _ = stateStorage.set(withoutError(nextState))
      replies = renderer.render(nextState, lastMessageStorage.get, lastPhotosStorage.get)
      _ <- requestLogged(replies)
    } yield ()

  private def withoutError(state: State): State =
    state match {
      case Error(returnTo, _) => returnTo
      case _                  => state
    }

  // internal

  private def requestLogged(reqs: Seq[Request[?]], saveMessageId: Boolean = true): F[Unit] =
    reqs
      .traverse[F, Any] {
        case my: SendMediaGroup   => telegramClient.sendMediaGroup(my).asInstanceOf[F[Any]]
        case my: EditMessageMedia => telegramClient.editMessageMedia(my).asInstanceOf[F[Any]]
        case req                  => request(req).asInstanceOf[F[Any]]
      }
      .map { results =>
        reqs.zip(results).map {

          case (req: DeleteMessage, deleted: Boolean) =>
            val chatId = parseChatId(req.chatId)
            val lastSentPhotos = lastPhotosStorage
              .getRaw(chatId)
              .getOrElse(Seq.empty)
              .filter(_ != req.messageId)
            lastPhotosStorage.setRaw(Some(lastSentPhotos))(chatId)
            println(s"Delete: $deleted")

          case (_, edited: Either[Boolean, Message]) =>
            println(s"Edit $edited")

          case (_, sent: Message) =>
            if (saveMessageId) {
              lastMessageStorage.set(Some(sent.messageId))(sent)
            }
            println(s"Sent $sent")

          case (_, messages: Array[Message]) =>
            if (messages.nonEmpty && saveMessageId) {
              lastPhotosStorage.set(Some(messages.map(_.messageId)))(messages.head)
            }
            println(s"Array ${messages.mkString("\n")}")

          case (_, any) =>
            println(s"Any $any")
        }
      }

  private def parseChatId(chatId: ChatId): ChatID =
    chatId match {
      case ChatId.Chat(id)   => id
      case ChatId.Channel(_) => sys.error("channels are not supported")
    }

  private def fail(implicit message: Message): F[Unit] =
    reply("Не понял вас :(").void

  private def safe[A](block: => A, default: A): A =
    Try {
      block
    }.recover { error =>
      logger.error("Bot failed", error)
      default
    }.get
}
