package com.github.mmvpm.bot.client.ofs

import cats.data.EitherT
import cats.MonadThrow
import cats.implicits.{catsSyntaxApplicativeError, toBifunctorOps, toFunctorOps}
import com.github.mmvpm.model.{OfferDescription, OfferID, Session}
import com.github.mmvpm.bot.OfsConfig
import com.github.mmvpm.bot.client.ofs.request._
import com.github.mmvpm.bot.client.ofs.response._
import com.github.mmvpm.bot.client.ofs.error._
import io.circe.generic.auto._
import com.github.mmvpm.bot.client.ofs.util.CirceInstances._
import com.github.mmvpm.bot.model.{OfferPatch, TgPhoto}
import io.circe.Error
import sttp.client3._
import sttp.client3.circe._

class OfsClientSttp[F[_]: MonadThrow](ofsConfig: OfsConfig, sttpBackend: SttpBackend[F, Any]) extends OfsClient[F] {

  def signUp(name: String, login: String, password: String): EitherT[F, OfsClientError, SignUpResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/auth/sign-up"
    val request = SignUpRequest(name, login, password)

    val response = basicRequest
      .post(requestUri)
      .body(request)
      .response(asJsonEither[OfsApiClientError, SignUpResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def signIn(login: String, password: String): EitherT[F, OfsClientError, SignInResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/auth/sign-in"

    val response = basicRequest
      .post(requestUri)
      .auth
      .basic(login, password)
      .response(asJsonEither[OfsApiClientError, SignInResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def whoami(session: Session): EitherT[F, OfsClientError, UserIdResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/auth/whoami/$session"

    val response = basicRequest
      .get(requestUri)
      .response(asJsonEither[OfsApiClientError, UserIdResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def search(query: String): EitherT[F, OfsClientError, OfferIdsResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/search?query=$query"

    val response = basicRequest
      .get(requestUri)
      .response(asJsonEither[OfsApiClientError, OfferIdsResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def getOffer(offerId: OfferID): EitherT[F, OfsClientError, OfferResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/$offerId"

    val response = basicRequest
      .get(requestUri)
      .response(asJsonEither[OfsApiClientError, OfferResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def getOffers(offerIds: Seq[OfferID]): EitherT[F, OfsClientError, OffersResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/list"

    val response = basicRequest
      .post(requestUri)
      .body(GetOffersRequest(offerIds.toList))
      .response(asJsonEither[OfsApiClientError, OffersResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def getMyOffers(session: Session): EitherT[F, OfsClientError, OffersResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/list/my"

    val response = basicRequest
      .get(requestUri)
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[OfsApiClientError, OffersResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def createOffer(
      session: Session,
      description: OfferDescription
  ): EitherT[F, OfsClientError, CreateOfferResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer"
    val request = CreateOfferRequest(description, source = None)

    val response = basicRequest
      .post(requestUri)
      .body(request)
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[OfsApiClientError, CreateOfferResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def updateOffer(session: Session, offerId: OfferID, patch: OfferPatch): EitherT[F, OfsClientError, OfferResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/$offerId"

    val response = basicRequest
      .put(requestUri)
      .body(patch.toUpdateOfferRequest)
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[OfsApiClientError, OfferResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def deleteOffer(session: Session, offerId: OfferID): EitherT[F, OfsClientError, OkResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/$offerId"

    val response = basicRequest
      .delete(requestUri)
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[OfsApiClientError, OkResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def addPhotos(session: Session, offerId: OfferID, photos: Seq[TgPhoto]): EitherT[F, OfsClientError, OfferResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/$offerId/photo"
    val request = AddOfferPhotosRequest(
      photos.collect { case TgPhoto(Some(url), _) => url.toString },
      photos.collect { case TgPhoto(_, Some(telegramId)) => telegramId }
    )

    val response = basicRequest
      .put(requestUri)
      .body(request)
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[OfsApiClientError, OfferResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  def deleteAllPhotos(session: Session, offerId: OfferID): EitherT[F, OfsClientError, OkResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/$offerId/photo/all"

    val response = basicRequest
      .delete(requestUri)
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[OfsApiClientError, OkResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))
      .recover(error => Left(OfsUnknownClientError(error.getMessage)))

    EitherT(response)
  }

  // internal

  private def parseFailure: ResponseException[OfsApiClientError, Error] => OfsClientError = {
    case HttpError(body, _) => body
    case error              => OfsUnknownClientError(error.getMessage)
  }
}
