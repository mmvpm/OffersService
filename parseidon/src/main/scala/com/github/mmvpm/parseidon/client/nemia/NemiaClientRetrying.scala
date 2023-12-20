package com.github.mmvpm.parseidon.client.nemia

import cats.data.EitherT
import cats.MonadThrow
import com.github.mmvpm.model.{OfferDescription, Session, UserDescriptionRaw}
import com.github.mmvpm.parseidon.client.nemia.error.NemiaError
import com.github.mmvpm.parseidon.client.nemia.response.{CreateOfferResponse, SignInResponse, SignUpResponse}
import com.github.mmvpm.parseidon.client.util.RetryUtils
import retry.{Sleep, retryingOnSomeErrors}

class NemiaClientRetrying[F[_]: MonadThrow: Sleep](nemiaClient: NemiaClient[F], retryUtils: RetryUtils[F])
    extends NemiaClient[F] {

  override def signUp(user: UserDescriptionRaw): EitherT[F, NemiaError, SignUpResponse] =
    retry(nemiaClient.signUp(user))

  override def signIn(login: String, password: String): EitherT[F, NemiaError, SignInResponse] =
    retry(nemiaClient.signIn(login, password))

  override def createOffer(session: Session, offer: OfferDescription): EitherT[F, NemiaError, CreateOfferResponse] =
    retry(nemiaClient.createOffer(session, offer))

  private def retry[Response](call: EitherT[F, NemiaError, Response]): EitherT[F, NemiaError, Response] =
    EitherT {
      retryingOnSomeErrors[Either[NemiaError, Response]](
        policy = retryUtils.policy,
        isWorthRetrying = retryUtils.isSttpClientException,
        onError = retryUtils.onError
      )(call.value)
    }
}
