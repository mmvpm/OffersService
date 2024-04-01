package com.github.mmvpm.bot.client.ofs.util

import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.model._
import io.circe._
import io.circe.generic.semiauto._

import java.util.UUID

case object CirceInstances {

  // common

  implicit val decoderUUID: Decoder[UUID] = Decoder[String].map(UUID.fromString)
  implicit val encoderUUID: Encoder[UUID] = Encoder[String].contramap(_.toString)

  // model

  implicit val decoderUserStatus: Decoder[UserStatus] = Decoder.decodeEnumeration(UserStatus)
  implicit val encoderUserStatus: Encoder[UserStatus] = Encoder.encodeEnumeration(UserStatus)

  implicit val decoderUser: Decoder[User] = deriveDecoder
  implicit val encoderUser: Encoder[User] = deriveEncoder

  implicit val decoderOfferStatus: Decoder[OfferStatus] = Decoder.decodeEnumeration(OfferStatus)
  implicit val encoderOfferStatus: Encoder[OfferStatus] = Encoder.encodeEnumeration(OfferStatus)

  implicit val decoderOfferDescription: Decoder[OfferDescription] = deriveDecoder
  implicit val encoderOfferDescription: Encoder[OfferDescription] = deriveEncoder

  implicit val decoderOffer: Decoder[Offer] = deriveDecoder
  implicit val encoderOffer: Encoder[Offer] = deriveEncoder
}
