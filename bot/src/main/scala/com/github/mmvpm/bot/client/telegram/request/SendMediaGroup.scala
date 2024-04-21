package com.github.mmvpm.bot.client.telegram.request

import com.bot4s.telegram.methods.Request
import com.bot4s.telegram.models.Message

case class SendMediaGroup(chatId: String, photos: Array[InputMediaPhoto]) extends Request[Array[Message]]
