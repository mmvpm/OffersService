package com.github.mmvpm.service.api.util

import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.model._
import com.github.mmvpm.service.api.request._
import com.github.mmvpm.service.api.response._
import sttp.tapir.Schema

object SchemaInstances {

  // model

  implicit val schemaOfferStatus: Schema[OfferStatus] =
    Schema.derivedEnumerationValue.description("Статус объявления")

  implicit val schemaOfferDescription: Schema[OfferDescription] =
    Schema.derived.description("Задаваемые пользователем поля объявления")

  implicit val schemaOffer: Schema[Offer] =
    Schema.derived.description("Объявление")

  implicit val schemaUserStatus: Schema[UserStatus] =
    Schema.derivedEnumerationValue.description("Статус пользователя")

  implicit val schemaUser: Schema[User] =
    Schema.derived.description("Пользователь")

  // requests

  implicit val schemaSignUpRequest: Schema[SignUpRequest] =
    Schema.derived.description("Запрос на регистрацию пользователя")

  implicit val schemaUpdateOfferRequest: Schema[UpdateOfferRequest] =
    Schema.derived.description("Запрос на изменение объявления")

  implicit val schemaCreateOfferRequest: Schema[CreateOfferRequest] =
    Schema.derived.description("Запрос на создание объявления")

  // responses

  implicit val schemaOkResponse: Schema[OkResponse] =
    Schema.derived.description("Успех")

  implicit val schemaSessionResponse: Schema[SessionResponse] =
    Schema.derived.description("Сессия пользователя")

  implicit val schemaUserResponse: Schema[UserResponse] =
    Schema.derived.description("Пользователь")

  implicit val schemaUserIdResponse: Schema[UserIdResponse] =
    Schema.derived.description("ID пользователя")

  implicit val schemaOfferResponse: Schema[OfferResponse] =
    Schema.derived.description("Объявление")

  implicit val schemaOffersResponse: Schema[OffersResponse] =
    Schema.derived.description("Список объявлений")
}
