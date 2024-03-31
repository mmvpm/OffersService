package com.github.mmvpm.service.api.util

import com.github.mmvpm.service.api.request._
import com.github.mmvpm.service.api.response._
import com.github.mmvpm.model._
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
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

  // requests

  implicit val decoderUpdateOfferRequest: Decoder[UpdateOfferRequest] = deriveDecoder
  implicit val encoderUpdateOfferRequest: Encoder[UpdateOfferRequest] = deriveEncoder

  implicit val decoderSignUpRequest: Decoder[SignUpRequest] = deriveDecoder
  implicit val encoderSignUpRequest: Encoder[SignUpRequest] = deriveEncoder

  implicit val decoderCreateOfferRequest: Decoder[CreateOfferRequest] = deriveDecoder
  implicit val encoderCreateOfferRequest: Encoder[CreateOfferRequest] = deriveEncoder

  // responses

  implicit val decoderOkResponse: Decoder[OkResponse] = deriveDecoder
  implicit val encoderOkResponse: Encoder[OkResponse] = deriveEncoder

  implicit val decoderSessionResponse: Decoder[SessionResponse] = deriveDecoder
  implicit val encoderSessionResponse: Encoder[SessionResponse] = deriveEncoder

  implicit val decoderUserResponse: Decoder[UserResponse] = deriveDecoder
  implicit val encoderUserResponse: Encoder[UserResponse] = deriveEncoder

  implicit val decoderUserIdResponse: Decoder[UserIdResponse] = deriveDecoder
  implicit val encoderUserIdResponse: Encoder[UserIdResponse] = deriveEncoder

  implicit val decoderOffersResponse: Decoder[OffersResponse] = deriveDecoder
  implicit val encoderOffersResponse: Encoder[OffersResponse] = deriveEncoder

  implicit val decoderOfferResponse: Decoder[OfferResponse] = deriveDecoder
  implicit val encoderOfferResponse: Encoder[OfferResponse] = deriveEncoder
}
