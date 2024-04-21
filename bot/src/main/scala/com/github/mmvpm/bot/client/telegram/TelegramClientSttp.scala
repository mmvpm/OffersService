package com.github.mmvpm.bot.client.telegram

import cats.MonadThrow
import cats.implicits.toFunctorOps
import com.bot4s.telegram.marshalling
import com.bot4s.telegram.marshalling.CirceDecoders
import com.bot4s.telegram.methods.Response
import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.client.telegram.request.{EditMessageMedia, InputMediaPhoto, SendMediaGroup}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import sttp.client3.{ResponseAs, SttpBackend, UriContext, asStringAlways, basicRequest, multipart}

class TelegramClientSttp[F[_]: MonadThrow](token: String, sttpBackend: SttpBackend[F, Any])
    extends TelegramClient[F]
    with CirceDecoders {

  def sendMediaGroup(request: SendMediaGroup)(implicit ev: Encoder[InputMediaPhoto]): F[Array[Message]] =
    basicRequest
      .post(uri"https://api.telegram.org/bot$token/SendMediaGroup")
      .multipartBody(
        multipart("chat_id", request.chatId),
        multipart("media", request.photos.asJson.toString)
      )
      .response(asJson[Response[Array[Message]]])
      .send(sttpBackend)
      .map(_.body.result)
      .map {
        case Some(array) => array
        case None        => println(s"Telegram error: error"); Array.empty
      }

  def editMessageMedia(request: EditMessageMedia)(implicit ev: Encoder[InputMediaPhoto]): F[Either[Boolean, Message]] =
    basicRequest
      .post(uri"https://api.telegram.org/bot$token/EditMessageMedia")
      .multipartBody(
        multipart("chat_id", request.chatId),
        multipart("message_id", request.messageId.toString),
        multipart("media", request.media.asJson.toString)
      )
      .response(asJson[Response[Either[Boolean, Message]]])
      .send(sttpBackend)
      .map(_.body.result)
      .map {
        case Some(either) => either
        case None         => println(s"Telegram error: error"); Left(false)
      }

  private def asJson[B: Decoder]: ResponseAs[B, Any] =
    asStringAlways("utf-8").map(s => marshalling.fromJson[B](s))
}
