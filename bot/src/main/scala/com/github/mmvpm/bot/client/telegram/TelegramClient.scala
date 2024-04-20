package com.github.mmvpm.bot.client.telegram

import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.client.telegram.request.{InputMediaPhoto, SendMediaGroup}
import io.circe.Encoder

trait TelegramClient[F[_]] {
  def sendMediaGroup(request: SendMediaGroup)(implicit ev: Encoder[InputMediaPhoto]): F[Array[Message]]
}
