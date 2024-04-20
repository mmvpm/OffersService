package com.github.mmvpm.bot.client.ofs

import cats.data.EitherT
import com.github.mmvpm.bot.client.ofs.error.OfsClientError
import com.github.mmvpm.bot.client.ofs.response._
import com.github.mmvpm.bot.model.{OfferPatch, TgPhoto}
import com.github.mmvpm.model.{OfferDescription, OfferID, Session}

trait OfsClient[F[_]] {

  def signUp(name: String, login: String, password: String): EitherT[F, OfsClientError, SignUpResponse]
  def signIn(login: String, password: String): EitherT[F, OfsClientError, SignInResponse]
  def whoami(session: Session): EitherT[F, OfsClientError, UserIdResponse]

  def search(query: String): EitherT[F, OfsClientError, OfferIdsResponse]
  def getOffer(offerId: OfferID): EitherT[F, OfsClientError, OfferResponse]
  def getOffers(offerIds: Seq[OfferID]): EitherT[F, OfsClientError, OffersResponse]
  def getMyOffers(session: Session): EitherT[F, OfsClientError, OffersResponse]
  def createOffer(session: Session, description: OfferDescription): EitherT[F, OfsClientError, CreateOfferResponse]
  def updateOffer(session: Session, offerId: OfferID, patch: OfferPatch): EitherT[F, OfsClientError, OfferResponse]
  def deleteOffer(session: Session, offerId: OfferID): EitherT[F, OfsClientError, OkResponse]
  def addPhotos(session: Session, offerId: OfferID, photos: Seq[TgPhoto]): EitherT[F, OfsClientError, OfferResponse]
  def deleteAllPhotos(session: Session, offerId: OfferID): EitherT[F, OfsClientError, OkResponse]
}
