package com.github.mmvpm.bot.render

import com.bot4s.telegram.methods.{DeleteMessage, EditMessageText, Request, SendMessage}
import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message}
import com.github.mmvpm.bot.client.telegram.request.{EditMessageMedia, InputMediaPhoto, SendMediaGroup}
import com.github.mmvpm.bot.model.MessageID
import com.github.mmvpm.bot.state.State
import com.github.mmvpm.bot.state.State._

class RendererImpl extends Renderer {

  def render(
      state: State,
      editMessage: Option[MessageID] = None,
      photoIds: Option[Seq[MessageID]]
  )(implicit message: Message): Seq[Request[?]] = {
    val buttons = state.next.map(_.map { tag =>
      InlineKeyboardButton.callbackData(buttonBy(tag), tag)
    })
    val markup = Some(InlineKeyboardMarkup(buttons))

    lazy val send = SendMessage(message.source, state.text, replyMarkup = markup)
    lazy val edit = EditMessageText(Some(message.source), editMessage, text = state.text, replyMarkup = markup)

    val photos = renderPhotos(state, photoIds)
    val isSendMediaGroup = photos.size == 1 && photos.head.isInstanceOf[SendMediaGroup]

    // the last message was sent by the bot
    val basic = if (editMessage.contains(message.messageId) && !isSendMediaGroup) edit else send

    photos ++ Seq(basic)
  }

  private def renderPhotos(
      state: State,
      optPhotoIds: Option[Seq[MessageID]]
  )(implicit message: Message): Seq[Request[?]] =
    state match {
      case state: WithPhotos =>
        val photos = state.photos.map { photo =>
          InputMediaPhoto(photo.media, None)
        }
        optPhotoIds match {
          case Some(photoIds) if photoIds.nonEmpty && photoIds.last == message.messageId - 1 =>
            val edits = photoIds.zip(photos).map { case (id, newPhoto) =>
              EditMessageMedia(message.source.toString, id, newPhoto)
            }
            if (photoIds.size == photos.size) {
              edits // edit all sent photos
            } else if (photoIds.size > photos.size) {
              val deletes = photoIds.drop(photos.size).map(DeleteMessage(message.source, _))
              edits ++ deletes
            } else {
              Seq(SendMediaGroup(message.source.toString, photos.toArray)) // send a new message
            }
          case _ =>
            Seq(SendMediaGroup(message.source.toString, photos.toArray))
        }
      case _ =>
        Seq.empty
    }
}
