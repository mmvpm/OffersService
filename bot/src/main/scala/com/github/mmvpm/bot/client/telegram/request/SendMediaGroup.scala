package com.github.mmvpm.bot.client.telegram.request

import com.bot4s.telegram.methods.Request

case class SendMediaGroup(chatId: String, photos: Array[InputMediaPhoto]) extends Request[Unit]
