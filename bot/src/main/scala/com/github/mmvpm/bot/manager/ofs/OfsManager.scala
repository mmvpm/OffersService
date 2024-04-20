package com.github.mmvpm.bot.manager.ofs

import cats.data.EitherT
import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.client.ofs.response.OfsOffer
import com.github.mmvpm.bot.manager.ofs.error.OfsError
import com.github.mmvpm.bot.manager.ofs.response.LoginOrRegisterResponse
import com.github.mmvpm.bot.model.{OfferPatch, TgPhoto}
import com.github.mmvpm.model.{Offer, OfferDescription, OfferID}

trait OfsManager[F[_]] {
  def login(implicit message: Message): EitherT[F, OfsError, LoginOrRegisterResponse.LoggedIn]
  def loginOrRegister(implicit message: Message): EitherT[F, OfsError, LoginOrRegisterResponse]

  def search(query: String): EitherT[F, OfsError, List[Offer]]
  def getOffer(offerId: OfferID): EitherT[F, OfsError, Option[Offer]]
  def getOffers(offerIds: Seq[OfferID]): EitherT[F, OfsError, List[Offer]]
  def getMyOffers(implicit message: Message): EitherT[F, OfsError, List[Offer]]
  def createOffer(description: OfferDescription)(implicit message: Message): EitherT[F, OfsError, OfsOffer]
  def updateOffer(offerId: OfferID, patch: OfferPatch)(implicit message: Message): EitherT[F, OfsError, Unit]
  def deleteOffer(offerId: OfferID)(implicit message: Message): EitherT[F, OfsError, Unit]
  def addOfferPhotos(offerId: OfferID, photos: Seq[TgPhoto])(implicit message: Message): EitherT[F, OfsError, Unit]
  def deleteAllPhotos(offerId: OfferID)(implicit message: Message): EitherT[F, OfsError, Unit]
}
