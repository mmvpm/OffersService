package com.github.mmvpm.nemia.api.util.request

import cats.effect.IO
import cats.implicits.toBifunctorOps
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.error.ApiError
import com.github.mmvpm.nemia.api.error.CirceInstances._
import com.github.mmvpm.nemia.api.request._
import com.github.mmvpm.nemia.api.response._
import com.github.mmvpm.nemia.api.util.{ConfigSupport, JsonUtils}
import com.github.mmvpm.nemia.api.util.CirceInstances._
import com.github.mmvpm.nemia.api.SessionHeaderName
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.client3.circe._

trait OfferRequestsSupport extends ConfigSupport {

  def getOffer(offerId: OfferID)(backend: SttpBackend[IO, Any]): IO[Either[ApiError, OfferResponse]] =
    basicRequest
      .get(uri"$baseUrl/api/v1/offer/$offerId")
      .response(asJsonEither[ApiError, OfferResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))

  def createOffer(description: OfferDescription)(backend: SttpBackend[IO, Any]): IO[Either[ApiError, OfferResponse]] =
    basicRequest
      .post(uri"$baseUrl/api/v1/offer")
      .body(CreateOfferRequest(description))
      .response(asJsonEither[ApiError, OfferResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))

  def updateOffer(
      session: Session,
      offerId: OfferID,
      request: UpdateOfferRequest
    )(backend: SttpBackend[IO, Any]): IO[Either[ApiError, OfferResponse]] =
    basicRequest
      .put(uri"$baseUrl/api/v1/offer/$offerId")
      .header(SessionHeaderName, session.toString)
      .body(request)
      .response(asJsonEither[ApiError, OfferResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))

  def deleteOffer(session: Session, offerId: OfferID)(backend: SttpBackend[IO, Any]): IO[Either[ApiError, OkResponse]] =
    basicRequest
      .delete(uri"$baseUrl/api/v1/offer/$offerId")
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[ApiError, OkResponse])
      .send(backend)
      .map(_.body.leftMap(JsonUtils.parseFailure))
}
