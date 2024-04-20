package com.github.mmvpm.service.dao.offer

import cats.data.EitherT
import com.github.mmvpm.model.{Offer, OfferID, Photo, UserID}
import com.github.mmvpm.service.dao.error.OfferDaoError
import com.github.mmvpm.service.dao.schema.OfferPatch

trait OfferDao[F[_]] {
  def getOffer(offerId: OfferID): EitherT[F, OfferDaoError, Offer]
  def getOffers(offerIds: List[OfferID]): EitherT[F, OfferDaoError, List[Offer]]
  def getOffersByUser(userId: UserID): EitherT[F, OfferDaoError, List[Offer]]
  def createOffer(offer: Offer): EitherT[F, OfferDaoError, Unit]
  def updateOffer(userId: UserID, offerId: OfferID, patch: OfferPatch): EitherT[F, OfferDaoError, Unit]
  def addPhotos(userId: UserID, offerId: OfferID, photos: Seq[Photo]): EitherT[F, OfferDaoError, Unit]
  def deleteAllPhotos(userId: UserID, offerId: OfferID): EitherT[F, OfferDaoError, Unit]
}
