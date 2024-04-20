package com.github.mmvpm.bot.client.telegram.request

import io.circe.{Encoder, Json}

case class InputMediaPhoto(media: String, caption: Option[String] = None)

object InputMediaPhoto {
  implicit val inputMediaPhotoEncoder: Encoder[InputMediaPhoto] =
    (photo: InputMediaPhoto) => {
      val captionField = photo.caption match {
        case Some(caption) => Seq(("caption", Json.fromString(caption)))
        case None          => Seq.empty
      }
      val fields = Seq(
        ("media", Json.fromString(photo.media)),
        ("type", Json.fromString("photo"))
      ) ++ captionField
      Json.obj(fields: _*)
    }
}
