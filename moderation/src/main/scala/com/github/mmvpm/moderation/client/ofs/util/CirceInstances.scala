package com.github.mmvpm.moderation.client.ofs.util

import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.model._
import io.circe._
import io.circe.generic.semiauto._

import java.net.{URI, URL}
import java.util.UUID

case object CirceInstances {

  // common

  implicit val decoderUUID: Decoder[UUID] = Decoder[String].map(UUID.fromString)
  implicit val encoderUUID: Encoder[UUID] = Encoder[String].contramap(_.toString)

  implicit val decoderURL: Decoder[URL] = Decoder[String].map(new URI(_).toURL)
  implicit val encoderURL: Encoder[URL] = Encoder[String].contramap(_.toString)

  // model

  implicit val codecUserStatus: Codec[UserStatus] = Codec.codecForEnumeration(UserStatus)

  implicit val codecUser: Codec[User] = deriveCodec

  implicit val codecOfferStatus: Codec[OfferStatus] = Codec.codecForEnumeration(OfferStatus)

  implicit val codecOfferDescription: Codec[OfferDescription] = deriveCodec

  implicit val codecPhoto: Codec[Photo] = deriveCodec

  implicit val codecOffer: Codec[Offer] = deriveCodec
}
