package com.github.mmvpm.parseidon.client.nemia

import cats.data.EitherT
import cats.MonadThrow
import cats.implicits.{toBifunctorOps, toFunctorOps}
import com.github.mmvpm.model.{OfferDescription, Session, UserDescriptionRaw}
import com.github.mmvpm.parseidon.NemiaConfig
import com.github.mmvpm.parseidon.client.nemia.request._
import com.github.mmvpm.parseidon.client.nemia.response._
import com.github.mmvpm.parseidon.client.util.CirceUtils._
import com.github.mmvpm.parseidon.client.SessionHeaderName
import com.github.mmvpm.parseidon.client.nemia.error._
import io.circe.generic.auto._
import io.circe.Error
import sttp.client3._
import sttp.client3.circe._

class NemiaClientSttp[F[_]: MonadThrow](nemiaConfig: NemiaConfig, sttpBackend: SttpBackend[F, Any])
    extends NemiaClient[F] {

  override def signUp(user: UserDescriptionRaw): EitherT[F, NemiaError, SignUpResponse] = {
    val requestUri = uri"${nemiaConfig.baseUrl}/api/v1/auth/sign-up"
    val request = SignUpRequest(user)

    val response = basicRequest
      .post(requestUri)
      .body(request)
      .response(asJsonEither[NemiaApiError, SignUpResponse])
      .readTimeout(nemiaConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))

    EitherT(response)
  }

  override def signIn(login: String, password: String): EitherT[F, NemiaError, SignInResponse] = {
    val requestUri = uri"${nemiaConfig.baseUrl}/api/v1/auth/sign-in"

    val response = basicRequest
      .post(requestUri)
      .auth
      .basic(login, password)
      .response(asJsonEither[NemiaApiError, SignInResponse])
      .readTimeout(nemiaConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))

    EitherT(response)
  }

  override def createOffer(
      session: Session,
      description: OfferDescription
  ): EitherT[F, NemiaError, CreateOfferResponse] = {
    val requestUri = uri"${nemiaConfig.baseUrl}/api/v1/offer"
    val request = CreateOfferRequest(description)

    val response = basicRequest
      .post(requestUri)
      .body(request)
      .header(SessionHeaderName, session.toString)
      .response(asJsonEither[NemiaApiError, CreateOfferResponse])
      .readTimeout(nemiaConfig.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(parseFailure))

    EitherT(response)
  }

  private def parseFailure(error: ResponseException[NemiaApiError, Error]): NemiaError =
    error match {
      case HttpError(body, _) => body
      case _                  => NemiaUnknownError(error.getMessage)
    }
}
