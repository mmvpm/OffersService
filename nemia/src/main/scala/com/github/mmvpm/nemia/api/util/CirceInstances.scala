package com.github.mmvpm.nemia.api.util

import com.github.mmvpm.nemia.api.request._
import com.github.mmvpm.nemia.api.response._
import com.github.mmvpm.model._
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.util.StringUtils.RichString
import io.circe._
import io.circe.generic.semiauto._

import java.net.URL
import java.time.Instant
import java.util.UUID

case object CirceInstances {

  // common

  implicit val decoderInstant: Decoder[Instant] = Decoder[String].map(Instant.parse)
  implicit val encoderInstant: Encoder[Instant] = Encoder[String].contramap(_.toString)

  implicit val decoderURL: Decoder[URL] = Decoder[String].map(_.toURL)
  implicit val encoderURL: Encoder[URL] = Encoder[String].contramap(_.toString)

  implicit val decoderUUID: Decoder[UUID] = Decoder[String].map(UUID.fromString)
  implicit val encoderUUID: Encoder[UUID] = Encoder[String].contramap(_.toString)

  // model

  implicit val decoderMark: Decoder[Mark] = deriveDecoder
  implicit val encoderMark: Encoder[Mark] = deriveEncoder

  implicit val decoderRating: Decoder[Rating] = deriveDecoder
  implicit val encoderRating: Encoder[Rating] = deriveEncoder

  implicit val decoderUserStatus: Decoder[UserStatus] = Decoder.decodeEnumeration(UserStatus)
  implicit val encoderUserStatus: Encoder[UserStatus] = Encoder.encodeEnumeration(UserStatus)

  implicit val decoderPasswordHashed: Decoder[PasswordHashed] = deriveDecoder
  implicit val encoderPasswordHashed: Encoder[PasswordHashed] = deriveEncoder

  implicit val decoderUserDescriptionRaw: Decoder[UserDescriptionRaw] = deriveDecoder
  implicit val encoderUserDescriptionRaw: Encoder[UserDescriptionRaw] = deriveEncoder

  implicit val decoderApiUserDescription: Decoder[ApiUserDescription] = deriveDecoder
  implicit val encoderApiUserDescription: Encoder[ApiUserDescription] = deriveEncoder

  implicit val decoderApiUser: Decoder[ApiUser] = deriveDecoder
  implicit val encoderApiUser: Encoder[ApiUser] = deriveEncoder

  implicit val decoderPhoto: Decoder[Photo] = deriveDecoder
  implicit val encoderPhoto: Encoder[Photo] = deriveEncoder

  implicit val decoderOfferStatus: Decoder[OfferStatus] = Decoder.decodeEnumeration(OfferStatus)
  implicit val encoderOfferStatus: Encoder[OfferStatus] = Encoder.encodeEnumeration(OfferStatus)

  implicit val decoderOfferDescription: Decoder[OfferDescription] = deriveDecoder
  implicit val encoderOfferDescription: Encoder[OfferDescription] = deriveEncoder

  implicit val decoderOffer: Decoder[Offer] = deriveDecoder
  implicit val encoderOffer: Encoder[Offer] = deriveEncoder

  // requests

  implicit val decoderUpdateUserRequest: Decoder[UpdateUserRequest] = deriveDecoder
  implicit val encoderUpdateUserRequest: Encoder[UpdateUserRequest] = deriveEncoder

  implicit val decoderUpdateOfferRequest: Decoder[UpdateOfferRequest] = deriveDecoder
  implicit val encoderUpdateOfferRequest: Encoder[UpdateOfferRequest] = deriveEncoder

  implicit val decoderSignUpRequest: Decoder[SignUpRequest] = deriveDecoder
  implicit val encoderSignUpRequest: Encoder[SignUpRequest] = deriveEncoder

  implicit val decoderRateUserRequest: Decoder[RateUserRequest] = deriveDecoder
  implicit val encoderRateUserRequest: Encoder[RateUserRequest] = deriveEncoder

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
