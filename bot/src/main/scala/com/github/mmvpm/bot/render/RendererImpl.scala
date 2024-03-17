package com.github.mmvpm.bot.render

import com.bot4s.telegram.methods.{EditMessageText, SendMessage}
import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message}
import com.github.mmvpm.bot.model.MessageID
import com.github.mmvpm.bot.state.State
import com.github.mmvpm.bot.state.State._

class RendererImpl extends Renderer {

  override def render(
      state: State,
      editMessage: Option[MessageID] = None
  )(implicit message: Message): Either[EditMessageText, SendMessage] = {
    val buttons = state.next.map { tag =>
      InlineKeyboardButton.callbackData(buttonBy(tag), tag)
    }
    val markup = Some(InlineKeyboardMarkup.singleColumn(buttons))

    lazy val send = SendMessage(message.source, state.text, replyMarkup = markup)
    lazy val edit = EditMessageText(Some(message.source), editMessage, text = state.text, replyMarkup = markup)

    if (editMessage.contains(message.messageId)) // the last message was sent by the bot
      Left(edit)
    else
      Right(send)
  }
}
