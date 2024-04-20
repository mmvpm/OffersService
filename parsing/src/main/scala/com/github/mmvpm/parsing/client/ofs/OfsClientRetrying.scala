package com.github.mmvpm.parsing.client.ofs

import cats.MonadThrow
import cats.data.EitherT
import com.github.mmvpm.model.{OfferDescription, OfferID, Session}
import com.github.mmvpm.parsing.client.ofs.error.OfsError
import com.github.mmvpm.parsing.client.ofs.response.{OfferResponse, SignInResponse, SignUpResponse}
import com.github.mmvpm.parsing.client.util.RetryUtils
import retry.{Sleep, retryingOnSomeErrors}

import java.net.URL

class OfsClientRetrying[F[_]: MonadThrow: Sleep](ofsClient: OfsClient[F], retryUtils: RetryUtils[F])
    extends OfsClient[F] {

  def signUp(name: String, login: String, password: String): EitherT[F, OfsError, SignUpResponse] =
    retry(ofsClient.signUp(name, login, password))

  def signIn(login: String, password: String): EitherT[F, OfsError, SignInResponse] =
    retry(ofsClient.signIn(login, password))

  def createOffer(session: Session, offer: OfferDescription, source: URL): EitherT[F, OfsError, OfferResponse] =
    retry(ofsClient.createOffer(session, offer, source))

  def addPhotos(session: Session, offerId: OfferID, photoUrls: Seq[URL]): EitherT[F, OfsError, OfferResponse] =
    retry(ofsClient.addPhotos(session, offerId, photoUrls))

  private def retry[Response](call: EitherT[F, OfsError, Response]): EitherT[F, OfsError, Response] =
    EitherT {
      retryingOnSomeErrors[Either[OfsError, Response]](
        policy = retryUtils.policy,
        isWorthRetrying = retryUtils.isSttpClientException,
        onError = retryUtils.onError
      )(call.value)
    }
}
