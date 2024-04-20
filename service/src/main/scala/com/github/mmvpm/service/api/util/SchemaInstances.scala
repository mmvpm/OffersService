package com.github.mmvpm.service.api.util

import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.model._
import com.github.mmvpm.service.api.request._
import com.github.mmvpm.service.api.response._
import sttp.tapir.Schema

import java.net.URL

object SchemaInstances {

  // common

  implicit val schemaURL: Schema[URL] =
    Schema.string.description("url").encodedExample("https://ya.ru")

  // model

  implicit val schemaOfferStatus: Schema[OfferStatus] =
    Schema.derivedEnumerationValue.description("Offer status")

  implicit val schemaOfferDescription: Schema[OfferDescription] =
    Schema.derived.description("User-defined offer fields")

  implicit val schemaPhoto: Schema[Photo] =
    Schema.derived.description("Photo")

  implicit val schemaOffer: Schema[Offer] =
    Schema.derived.description("Offer")

  implicit val schemaUserStatus: Schema[UserStatus] =
    Schema.derivedEnumerationValue.description("User status")

  implicit val schemaUser: Schema[User] =
    Schema.derived.description("User")

  // requests

  implicit val schemaSignUpRequest: Schema[SignUpRequest] =
    Schema.derived.description("Request to register a new user")

  implicit val schemaUpdateOfferRequest: Schema[UpdateOfferRequest] =
    Schema.derived.description("Request to update the offer")

  implicit val schemaCreateOfferRequest: Schema[CreateOfferRequest] =
    Schema.derived.description("Request to create an offer")

  implicit val schemaGetOffersRequest: Schema[GetOffersRequest] =
    Schema.derived.description("Request to get offers by their ids")

  implicit val schemaAddOfferPhotosRequest: Schema[AddOfferPhotosRequest] =
    Schema.derived.description("Request to add photos to the offer")

  // responses

  implicit val schemaOkResponse: Schema[OkResponse] =
    Schema.derived.description("Success")

  implicit val schemaSessionResponse: Schema[SessionResponse] =
    Schema.derived.description("User session")

  implicit val schemaUserResponse: Schema[UserResponse] =
    Schema.derived.description("User")

  implicit val schemaUserIdResponse: Schema[UserIdResponse] =
    Schema.derived.description("User ID")

  implicit val schemaOfferResponse: Schema[OfferResponse] =
    Schema.derived.description("Offer")

  implicit val schemaOffersResponse: Schema[OffersResponse] =
    Schema.derived.description("List of offers")

  implicit val schemaOfferIdsResponse: Schema[OfferIdsResponse] =
    Schema.derived.description("List of offer IDs")
}
