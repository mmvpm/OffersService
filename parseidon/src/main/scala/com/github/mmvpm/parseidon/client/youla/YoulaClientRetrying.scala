package com.github.mmvpm.parseidon.client.youla

import cats.data.EitherT
import cats.MonadThrow
import com.github.mmvpm.parseidon.client.util.RetryUtils
import com.github.mmvpm.parseidon.client.youla.response.CatalogResponse
import retry.{Sleep, retryingOnSomeErrors}

class YoulaClientRetrying[F[_]: MonadThrow: Sleep](youlaClient: YoulaClient[F], retryUtils: RetryUtils[F])
    extends YoulaClient[F] {

  override def search(query: String): EitherT[F, String, CatalogResponse] =
    retry(youlaClient.search(query))

  private def retry[Response](call: EitherT[F, String, Response]): EitherT[F, String, Response] =
    EitherT {
      retryingOnSomeErrors[Either[String, Response]](
        policy = retryUtils.policy,
        isWorthRetrying = retryUtils.isSttpClientException,
        onError = retryUtils.onError
      )(call.value)
    }
}
