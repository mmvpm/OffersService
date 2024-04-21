package com.github.mmvpm.bot.client.telegram.request

import com.bot4s.telegram.methods.Request
import com.bot4s.telegram.models.Message

case class EditMessageMedia(chatId: String, messageId: Int, media: InputMediaPhoto)
    extends Request[Either[Boolean, Message]]
