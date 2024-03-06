package com.github.mmvpm.stub.api.util

import com.github.mmvpm.model._
import com.github.mmvpm.stub.api.request._
import com.github.mmvpm.stub.api.response._
import io.circe._
import io.circe.generic.semiauto._

case object CirceInstances {

  // model

  implicit val decoderStub: Decoder[Stub] = deriveDecoder
  implicit val encoderStub: Encoder[Stub] = deriveEncoder

  // requests

  implicit val decoderGetStubsRequest: Decoder[GetStubsRequest] = deriveDecoder
  implicit val encoderGetStubsRequest: Encoder[GetStubsRequest] = deriveEncoder

  implicit val decoderCreateStubRequest: Decoder[CreateStubRequest] = deriveDecoder
  implicit val encoderCreateStubRequest: Encoder[CreateStubRequest] = deriveEncoder

  implicit val decoderUpdateStubRequest: Decoder[UpdateStubRequest] = deriveDecoder
  implicit val encoderUpdateStubRequest: Encoder[UpdateStubRequest] = deriveEncoder

  // responses

  implicit val decoderOkResponse: Decoder[OkResponse] = deriveDecoder
  implicit val encoderOkResponse: Encoder[OkResponse] = deriveEncoder

  implicit val decoderStubsResponse: Decoder[StubsResponse] = deriveDecoder
  implicit val encoderStubsResponse: Encoder[StubsResponse] = deriveEncoder

  implicit val decoderStubResponse: Decoder[StubResponse] = deriveDecoder
  implicit val encoderStubResponse: Encoder[StubResponse] = deriveEncoder
}
