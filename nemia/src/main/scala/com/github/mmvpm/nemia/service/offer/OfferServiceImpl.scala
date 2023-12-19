package com.github.mmvpm.nemia.service.offer

import cats.data.EitherT
import cats.{Functor, Monad}
import cats.effect.std.UUIDGen
import cats.effect.Clock
import com.github.mmvpm.nemia.api.request.{CreateOfferRequest, UpdateOfferRequest}
import com.github.mmvpm.nemia.api.response.{OfferResponse, OffersResponse, OkResponse}
import com.github.mmvpm.nemia.dao.offer.OfferDao
import com.github.mmvpm.nemia.dao.DaoUpdate
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.error._
import com.github.mmvpm.nemia.dao.error._
import com.github.mmvpm.nemia.service.offer.OfferServiceImpl._

class OfferServiceImpl[F[_]: Monad: Clock: UUIDGen](offerDao: OfferDao[F]) extends OfferService[F] {

  override def getOffer(offerId: OfferID): EitherT[F, ApiError, OfferResponse] =
    getOfferRaw(offerId).map(OfferResponse).convertError

  override def getOffers(userId: UserID): EitherT[F, ApiError, OffersResponse] =
    offerDao.getOffersByUser(userId).map(OffersResponse).convertError

  override def createOffer(userId: UserID, request: CreateOfferRequest): EitherT[F, ApiError, OfferResponse] =
    (for {
      offerId <- EitherT.liftF(UUIDGen[F].randomUUID)
      now <- EitherT.liftF(Clock[F].realTimeInstant)
      offer = Offer(offerId, userId, request.description, OfferStatus.Active, now, now)
      _ <- offerDao.createOffer(offer)
    } yield OfferResponse(offer)).convertError

  override def updateOffer(
      userId: UserID,
      offerId: OfferID,
      request: UpdateOfferRequest
  ): EitherT[F, ApiError, OfferResponse] =
    (for {
      offer <- getOfferRaw(offerId)
      _ <- checkOfferBelonging(userId, offer)
      newOffer <- offerDao.updateOffer(offerId, updateOfferDaoFunc(request))
    } yield OfferResponse(newOffer)).convertError

  override def deleteOffer(userId: UserID, offerId: OfferID): EitherT[F, ApiError, OkResponse] =
    (for {
      offer <- getOfferRaw(offerId)
      _ <- checkOfferBelonging(userId, offer)
      _ <- offerDao.updateOffer(offerId, deleteOfferDaoFunc)
    } yield OkResponse()).convertError

  // internal

  private def checkOfferBelonging(userId: UserID, offer: Offer): EitherT[F, OfferDaoError, Unit] =
    EitherT.cond(userId == offer.userId, (), OfferBelongingFailedDaoError(offer.id, userId))

  private def getOfferRaw(offerId: OfferID): EitherT[F, OfferDaoError, Offer] =
    for {
      offers <- offerDao.getOffers(List(offerId))
      offer <- EitherT.fromOption(offers.headOption, OfferNotFoundDaoError(offerId): OfferDaoError)
    } yield offer

  private def updateOfferDaoFunc(request: UpdateOfferRequest)(old: Offer): DaoUpdate[Offer] = {
    val name = request.name.getOrElse(old.description.name)
    val text = request.text.getOrElse(old.description.text)
    val price = request.price.getOrElse(old.description.price)
    val photos = request.photos.getOrElse(old.description.photos)
    val newOffer = old.copy(description = OfferDescription(name, price, text, photos))
    DaoUpdate.SaveNew(newOffer)
  }

  private def deleteOfferDaoFunc(old: Offer): DaoUpdate[Offer] =
    if (old.status != OfferStatus.Banned) {
      DaoUpdate.SaveNew(old.copy(status = OfferStatus.Deleted))
    } else {
      // user can not 'unban' his offer by deleting it
      // moreover, 'banned' offer is the same as 'deleted' for other users
      DaoUpdate.DoNothing
    }
}

object OfferServiceImpl {

  private[service] implicit class RichOfferResponse[F[_]: Functor, T](response: EitherT[F, OfferDaoError, T]) {
    def convertError: EitherT[F, ApiError, T] = response.leftMap(offerConversion)
  }

  private[service] def offerConversion: OfferDaoError => ApiError = {
    case OfferNotFoundDaoError(offerId)           => OfferNotFoundApiError(offerId)
    case OfferAlreadyExistsDaoError(offerId)      => OfferAlreadyExistsApiError(offerId)
    case OfferBelongingFailedDaoError(offerId, _) => OfferNotFoundApiError(offerId)
    case error                                    => OfferDaoInternalApiError(error.details)
  }
}
