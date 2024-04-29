package com.github.mmvpm.moderation.client.ofs

import cats.MonadThrow
import cats.data.EitherT
import com.github.mmvpm.model.OfferID
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.moderation.client.ofs.error.OfsClientError
import com.github.mmvpm.moderation.client.ofs.response.{OffersResponse, OkResponse}
import com.github.mmvpm.moderation.client.util.RetryUtils
import retry.{Sleep, retryingOnSomeErrors}

class OfsClientRetrying[F[_]: MonadThrow: Sleep](ofsClient: OfsClient[F], retryUtils: RetryUtils[F])
    extends OfsClient[F] {

  def getOffersByStatus(status: OfferStatus, limit: Int): EitherT[F, OfsClientError, OffersResponse] =
    retry(ofsClient.getOffersByStatus(status, limit))

  def updateOfferStatusBatch(newStatuses: List[(OfferID, OfferStatus)]): EitherT[F, OfsClientError, OkResponse] =
    retry(ofsClient.updateOfferStatusBatch(newStatuses))

  private def retry[Response](call: EitherT[F, OfsClientError, Response]): EitherT[F, OfsClientError, Response] =
    EitherT {
      retryingOnSomeErrors[Either[OfsClientError, Response]](
        policy = retryUtils.policy,
        isWorthRetrying = retryUtils.isSttpClientException,
        onError = retryUtils.onError
      )(call.value)
    }
}
