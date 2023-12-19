package com.github.mmvpm.nemia.api.util

import com.github.mmvpm.nemia.api.request._
import com.github.mmvpm.nemia.api.response._
import com.github.mmvpm.model._
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
import sttp.tapir.Schema

import java.net.URL

object SchemaInstances {

  // common

  implicit val schemaURL: Schema[URL] =
    Schema.string.description("Ссылка").encodedExample("https://ya.ru")

  // model

  implicit val schemaPhoto: Schema[Photo] =
    Schema.derived.description("Фотография")

  implicit val schemaOfferStatus: Schema[OfferStatus] =
    Schema.derivedEnumerationValue.description("Статус объявления")

  implicit val schemaOfferDescription: Schema[OfferDescription] =
    Schema.derived.description("Задаваемые пользователем поля объявления")

  implicit val schemaOffer: Schema[Offer] =
    Schema.derived.description("Объявление")

  implicit val schemaMark: Schema[Mark] =
    Schema.derived.description("Поставленная пользователю оценка (от 1 до 10)")

  implicit val schemaRating: Schema[Rating] =
    Schema.derived.description("Рейтинг пользователя")

  implicit val schemaUserStatus: Schema[UserStatus] =
    Schema.derivedEnumerationValue.description("Статус пользователя")

  implicit val schemaPasswordHashed: Schema[PasswordHashed] =
    Schema.derived.description("Захэшированный пароль")

  implicit val schemaUserDescriptionRaw: Schema[UserDescriptionRaw] =
    Schema.derived.description("Профиль пользователя с открытым паролем")

  implicit val schemaApiUserDescription: Schema[ApiUserDescription] =
    Schema.derived.description("Профиль пользователя")

  implicit val schemaApiUser: Schema[ApiUser] =
    Schema.derived.description("Пользователь")

  // requests

  implicit val schemaSignUpRequest: Schema[SignUpRequest] =
    Schema.derived.description("Запрос на регистрацию пользователя")

  implicit val schemaRateUserRequest: Schema[RateUserRequest] =
    Schema.derived.description("Запрос на выставление оценки пользователю")

  implicit val schemaUpdateUserRequest: Schema[UpdateUserRequest] =
    Schema.derived.description("Запрос на изменение пользователя")

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
