package com.github.mmvpm.bot.client.telegram

import cats.MonadThrow
import cats.implicits.toFunctorOps
import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.client.telegram.request.{InputMediaPhoto, SendMediaGroup}
import com.github.mmvpm.bot.client.telegram.response.OkMessagesResponse
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import sttp.client3.circe.asJson
import sttp.client3.{SttpBackend, UriContext, basicRequest, multipart}

class TelegramClientSttp[F[_]: MonadThrow](token: String, sttpBackend: SttpBackend[F, Any]) extends TelegramClient[F] {

  def sendMediaGroup(request: SendMediaGroup)(implicit ev: Encoder[InputMediaPhoto]): F[Array[Message]] =
    basicRequest
      .post(uri"https://api.telegram.org/bot$token/SendMediaGroup")
      .multipartBody(
        multipart("chat_id", request.chatId),
        multipart("media", request.photos.asJson.toString)
      )
      .response(asJson[OkMessagesResponse])
      .send(sttpBackend)
      .map(_.body)
      .map {
        case Right(ok)   => ok.result
        case Left(error) => println(s"Telegram error: $error"); Array.empty
      }
}
