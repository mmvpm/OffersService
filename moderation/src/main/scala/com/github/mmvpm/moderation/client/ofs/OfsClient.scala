package com.github.mmvpm.moderation.client.ofs

import cats.MonadThrow
import cats.data.EitherT
import cats.implicits.{catsSyntaxApplicativeError, toBifunctorOps, toFunctorOps}
import com.github.mmvpm.model.OfferID
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.moderation.Config.OfsConfig
import com.github.mmvpm.moderation.client.ofs.error._
import com.github.mmvpm.moderation.client.ofs.request.UpdateOfferStatusBatchRequest
import com.github.mmvpm.moderation.client.ofs.response._
import com.github.mmvpm.moderation.client.ofs.util.CirceInstances._
import io.circe.Error
import io.circe.generic.auto._
import sttp.client3._
import sttp.client3.circe._

trait OfsClient[F[_]] {
  def getOffersByStatus(status: OfferStatus, limit: Int): EitherT[F, OfsClientError, OffersResponse]
  def updateOfferStatusBatch(newStatuses: List[(OfferID, OfferStatus)]): EitherT[F, OfsClientError, OkResponse]
}

object OfsClient {

  def sttp[F[_]: MonadThrow](config: OfsConfig, sttpBackend: SttpBackend[F, Any]): OfsClient[F] =
    new OfsClientSttp[F](config, sttpBackend)

  private final class OfsClientSttp[F[_]: MonadThrow](ofsConfig: OfsConfig, sttpBackend: SttpBackend[F, Any])
      extends OfsClient[F] {

    def getOffersByStatus(status: OfferStatus, limit: Int): EitherT[F, OfsClientError, OffersResponse] = {
      val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/list/status?status=$status&limit=$limit"

      val response = basicRequest
        .get(requestUri)
        .response(asJsonEither[OfsApiClientError, OffersResponse])
        .readTimeout(ofsConfig.requestTimeout)
        .send(sttpBackend)
        .map(_.body.leftMap(parseFailure))

      EitherT(response)
    }


    def updateOfferStatusBatch(newStatuses: List[(OfferID, OfferStatus)]): EitherT[F, OfsClientError, OkResponse] = {
      val requestUri = uri"${ofsConfig.baseUrl}/api/v1/offer/list/status"
      val body = UpdateOfferStatusBatchRequest.from(newStatuses)

      val response = basicRequest
        .put(requestUri)
        .body(body)
        .response(asJsonEither[OfsApiClientError, OkResponse])
        .readTimeout(ofsConfig.requestTimeout)
        .send(sttpBackend)
        .map(_.body.leftMap(parseFailure))

      EitherT(response)
    }

    // internal

    private def parseFailure: ResponseException[OfsApiClientError, Error] => OfsClientError = {
      case HttpError(body, _) => body
      case error              => OfsUnknownClientError(error.getMessage)
    }
  }
}
