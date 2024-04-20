package com.github.mmvpm.parsing.client.ofs

import cats.data.EitherT
import com.github.mmvpm.model.{OfferDescription, OfferID, Session}
import com.github.mmvpm.parsing.client.ofs.error.OfsError
import com.github.mmvpm.parsing.client.ofs.response.{OfferResponse, SignInResponse, SignUpResponse}

import java.net.URL

trait OfsClient[F[_]] {
  def signUp(name: String, login: String, password: String): EitherT[F, OfsError, SignUpResponse]
  def signIn(login: String, password: String): EitherT[F, OfsError, SignInResponse]
  def createOffer(session: Session, description: OfferDescription, source: URL): EitherT[F, OfsError, OfferResponse]
  def addPhotos(session: Session, offerId: OfferID, photoUrls: Seq[URL]): EitherT[F, OfsError, OfferResponse]
}
