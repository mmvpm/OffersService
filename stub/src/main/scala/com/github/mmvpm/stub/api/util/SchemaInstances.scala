package com.github.mmvpm.stub.api.util

import com.github.mmvpm.model._
import com.github.mmvpm.stub.api.request._
import com.github.mmvpm.stub.api.response._
import sttp.tapir.Schema

object SchemaInstances {

  // model

  implicit val schemaStub: Schema[Stub] =
    Schema.derived.description("Заглушка")

  // requests

  implicit val schemaGetStubsRequest: Schema[GetStubsRequest] =
    Schema.derived.description("Запрос на получение списка заглушек")

  implicit val schemaCreateStubRequest: Schema[CreateStubRequest] =
    Schema.derived.description("Запрос на создание заглушки")

  implicit val schemaUpdateStubRequest: Schema[UpdateStubRequest] =
    Schema.derived.description("Запрос на изменение заглушки")

  // responses

  implicit val schemaOkResponse: Schema[OkResponse] =
    Schema.derived.description("Успех")

  implicit val schemaStubResponse: Schema[StubResponse] =
    Schema.derived.description("Заглушка")

  implicit val schemaStubsResponse: Schema[StubsResponse] =
    Schema.derived.description("Список заглушек")
}
