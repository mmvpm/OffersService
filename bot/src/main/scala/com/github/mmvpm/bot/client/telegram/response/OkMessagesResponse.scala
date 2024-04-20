package com.github.mmvpm.bot.client.telegram.response

import com.bot4s.telegram.marshalling.CirceDecoders.messageDecoder
import com.bot4s.telegram.models.Message
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class OkMessagesResponse(ok: Boolean, result: Array[Message])

object OkMessagesResponse {
  implicit val codecUser: Decoder[OkMessagesResponse] = deriveDecoder
}
