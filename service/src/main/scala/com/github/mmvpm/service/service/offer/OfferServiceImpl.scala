package com.github.mmvpm.service.service.offer

import cats.data.{EitherT, NonEmptyList}
import cats.effect.std.UUIDGen
import cats.implicits.toFunctorOps
import cats.{Functor, Monad}
import com.github.mmvpm.model._
import com.github.mmvpm.service.api.error._
import com.github.mmvpm.service.api.request.{CreateOfferRequest, GetOffersRequest, UpdateOfferRequest}
import com.github.mmvpm.service.api.response.{OfferResponse, OffersResponse, OkResponse}
import com.github.mmvpm.service.dao.error._
import com.github.mmvpm.service.dao.offer.OfferDao
import com.github.mmvpm.service.dao.schema.OfferPatch
import com.github.mmvpm.service.service.offer.OfferServiceImpl._

class OfferServiceImpl[F[_]: Monad: UUIDGen](offerDao: OfferDao[F]) extends OfferService[F] {

  override def getOffer(offerId: OfferID): EitherT[F, ApiError, OfferResponse] =
    offerDao
      .getOffer(offerId)
      .map(OfferResponse)
      .convertError

  def getOffers(request: GetOffersRequest): EitherT[F, ApiError, OffersResponse] =
    offerDao
      .getOffers(request.offerIds)
      .map(OffersResponse)
      .convertError

  override def getOffers(userId: UserID): EitherT[F, ApiError, OffersResponse] =
    offerDao
      .getOffersByUser(userId)
      .map(OffersResponse)
      .convertError

  override def createOffer(userId: UserID, request: CreateOfferRequest): EitherT[F, ApiError, OfferResponse] =
    (for {
      offerId <- EitherT.liftF(UUIDGen[F].randomUUID)
      offer = Offer(offerId, userId, request.description, OfferStatus.Active, request.source)
      _ <- offerDao.createOffer(offer)
    } yield OfferResponse(offer)).convertError

  override def updateOffer(
      userId: UserID,
      offerId: OfferID,
      request: UpdateOfferRequest
  ): EitherT[F, ApiError, OfferResponse] =
    offerDao
      .updateOffer(userId, offerId, OfferPatch.from(request))
      .flatMap(_ => offerDao.getOffer(offerId))
      .map(OfferResponse)
      .convertError

  override def deleteOffer(userId: UserID, offerId: OfferID): EitherT[F, ApiError, OkResponse] =
    offerDao
      .updateOffer(userId, offerId, OfferPatch(status = Some(OfferStatus.Deleted)))
      .as(OkResponse())
      .convertError
}

object OfferServiceImpl {

  private[service] implicit class RichOfferResponse[F[_]: Functor, T](response: EitherT[F, OfferDaoError, T]) {
    def convertError: EitherT[F, ApiError, T] = response.leftMap(offerConversion)
  }

  private[service] def offerConversion: OfferDaoError => ApiError = {
    case OfferNotFoundDaoError(offerId) => OfferNotFoundApiError(offerId)
    case error                          => OfferDaoInternalApiError(error.details)
  }
}
