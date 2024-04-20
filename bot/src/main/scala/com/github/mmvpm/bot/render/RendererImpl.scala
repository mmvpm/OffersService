package com.github.mmvpm.bot.render

import com.bot4s.telegram.methods.{EditMessageText, Request, SendMessage}
import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message}
import com.github.mmvpm.bot.client.telegram.request.{InputMediaPhoto, SendMediaGroup}
import com.github.mmvpm.bot.model.MessageID
import com.github.mmvpm.bot.state.State
import com.github.mmvpm.bot.state.State._

class RendererImpl extends Renderer {

  def render(state: State, editMessage: Option[MessageID] = None)(implicit message: Message): Request[?] = {
    val buttons = state.next.map { tag =>
      InlineKeyboardButton.callbackData(buttonBy(tag, state), tag)
    }
    val markup = Some(InlineKeyboardMarkup.singleColumn(buttons))

    lazy val send = SendMessage(message.source, state.text, replyMarkup = markup)
    lazy val edit = EditMessageText(Some(message.source), editMessage, text = state.text, replyMarkup = markup)

    // the last message was sent by the bot
    if (editMessage.contains(message.messageId) && !state.isInstanceOf[WithPhotos]) edit else send
  }

  def renderPhotos(state: State)(implicit message: Message): Option[Request[?]] =
    state match {
      case state: WithPhotos =>
        val photos = state.photos.map { photo =>
          InputMediaPhoto(photo.media, None)
        }
        Some(SendMediaGroup(message.source.toString, photos.toArray))
      case _ =>
        None
    }
}
