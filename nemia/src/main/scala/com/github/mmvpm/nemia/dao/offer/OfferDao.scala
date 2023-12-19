package com.github.mmvpm.nemia.dao.offer

import cats.data.EitherT
import com.github.mmvpm.nemia.dao.DaoUpdate
import com.github.mmvpm.model.{Offer, OfferID, UserID}
import com.github.mmvpm.nemia.dao.error.OfferDaoError

trait OfferDao[F[_]] {
  def getOffers(offerIds: List[OfferID]): EitherT[F, OfferDaoError, List[Offer]]
  def getOffersByUser(userId: UserID): EitherT[F, OfferDaoError, List[Offer]]
  def createOffer(offer: Offer): EitherT[F, OfferDaoError, Unit]
  def updateOffer(offerId: OfferID, updateFunc: Offer => DaoUpdate[Offer]): EitherT[F, OfferDaoError, Offer]
}
