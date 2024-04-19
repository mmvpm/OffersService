package com.github.mmvpm.parsing.client.ofs

import cats.data.EitherT
import cats.MonadThrow
import cats.implicits.{toBifunctorOps, toFunctorOps}
import com.github.mmvpm.model.{OfferDescription, Session}
import com.github.mmvpm.parsing.OfsConfig
import com.github.mmvpm.parsing.client.ofs.request._
import com.github.mmvpm.parsing.client.ofs.response._
import com.github.mmvpm.parsing.client.SessionHeaderName
import com.github.mmvpm.parsing.client.ofs.error._
import io.circe.generic.auto._
import io.circe.Error
import sttp.client3._
import sttp.client3.circe._

class OfsClientSttp[F[_]: MonadThrow](ofsConfig: OfsConfig, sttpBackend: SttpBackend[F, Any])
    extends OfsClient[F] {

  def signUp(name: String, login: String, password: String): EitherT[F, OfsError, SignUpResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/auth/sign-up"
    val request = SignUpRequest(name, login, password)

    val response = basicRequest
      .post(requestUri)
      .body(request)
      .response(asJsonEither[OfsApiError, SignUpResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))

    EitherT(response)
  }

  override def signIn(login: String, password: String): EitherT[F, OfsError, SignInResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/auth/sign-in"

    val response = basicRequest
      .post(requestUri)
      .auth
      .basic(login, password)
      .response(asJsonEither[OfsApiError, SignInResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))

    EitherT(response)
  }

  override def createOffer(
      session: Session,
      description: OfferDescription
  ): EitherT[F, OfsError, CreateOfferResponse] = {
    val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer"
    val request = CreateOfferRequest(description)

    val response = basicRequest
      .post(requestUri)
      .body(request)
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[OfsApiError, CreateOfferResponse])
      .readTimeout(ofsConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))

    EitherT(response)
  }

  private def parseFailure: ResponseException[OfsApiError, Error] => OfsError = {
    case HttpError(body, _) => body
    case error              => OfsUnknownError(error.getMessage)
  }
}
