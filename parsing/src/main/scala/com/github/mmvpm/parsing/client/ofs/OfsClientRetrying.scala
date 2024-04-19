package com.github.mmvpm.parsing.client.ofs

import cats.MonadThrow
import cats.data.EitherT
import com.github.mmvpm.model.{OfferDescription, Session}
import com.github.mmvpm.parsing.client.ofs.error.OfsError
import com.github.mmvpm.parsing.client.ofs.response.{CreateOfferResponse, SignInResponse, SignUpResponse}
import com.github.mmvpm.parsing.client.util.RetryUtils
import retry.{Sleep, retryingOnSomeErrors}

class OfsClientRetrying[F[_]: MonadThrow: Sleep](ofsClient: OfsClient[F], retryUtils: RetryUtils[F])
    extends OfsClient[F] {

  def signUp(name: String, login: String, password: String): EitherT[F, OfsError, SignUpResponse] =
    retry(ofsClient.signUp(name, login, password))

  override def signIn(login: String, password: String): EitherT[F, OfsError, SignInResponse] =
    retry(ofsClient.signIn(login, password))

  override def createOffer(session: Session, offer: OfferDescription): EitherT[F, OfsError, CreateOfferResponse] =
    retry(ofsClient.createOffer(session, offer))

  private def retry[Response](call: EitherT[F, OfsError, Response]): EitherT[F, OfsError, Response] =
    EitherT {
      retryingOnSomeErrors[Either[OfsError, Response]](
        policy = retryUtils.policy,
        isWorthRetrying = retryUtils.isSttpClientException,
        onError = retryUtils.onError
      )(call.value)
    }
}
