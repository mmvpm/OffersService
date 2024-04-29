package com.github.mmvpm.service.service.offer

import cats.data.EitherT
import cats.effect.std.UUIDGen
import cats.implicits.{toFunctorOps, toTraverseOps}
import cats.{Functor, Monad}
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model._
import com.github.mmvpm.service.api.error._
import com.github.mmvpm.service.api.request._
import com.github.mmvpm.service.api.response.{OfferIdsResponse, OfferResponse, OffersResponse, OkResponse}
import com.github.mmvpm.service.dao.error._
import com.github.mmvpm.service.dao.offer.OfferDao
import com.github.mmvpm.service.dao.schema.OfferPatch
import com.github.mmvpm.service.service.offer.OfferServiceImpl._

import java.net.URL
import scala.util.Random

class OfferServiceImpl[F[_]: Monad: UUIDGen](offerDao: OfferDao[F]) extends OfferService[F] {

  def getOffer(offerId: OfferID): EitherT[F, ApiError, OfferResponse] =
    offerDao
      .getOffer(offerId)
      .map(OfferResponse)
      .convertError

  def getOffers(request: GetOffersRequest): EitherT[F, ApiError, OffersResponse] =
    offerDao
      .getOffers(request.offerIds)
      .map(OffersResponse)
      .convertError

  def getOffers(userId: UserID): EitherT[F, ApiError, OffersResponse] =
    offerDao
      .getOffersByUser(userId)
      .map(OffersResponse)
      .convertError

  def getOffers(status: OfferStatus, limit: Int): EitherT[F, ApiError, OffersResponse] =
    offerDao
      .getOffersByStatus(status, limit)
      .map(OffersResponse)
      .convertError

  def createOffer(userId: UserID, request: CreateOfferRequest): EitherT[F, ApiError, OfferResponse] =
    (for {
      offerId <- EitherT.liftF(UUIDGen[F].randomUUID)
      offer = Offer(offerId, userId, request.description, OfferStatus.OnModeration, request.source, Seq.empty)
      _ <- offerDao.createOffer(offer)
    } yield OfferResponse(offer)).convertError

  def updateOffer(
      userId: UserID,
      offerId: OfferID,
      request: UpdateOfferRequest
  ): EitherT[F, ApiError, OfferResponse] =
    offerDao
      .updateOffer(userId, offerId, OfferPatch.from(request))
      .flatMap(_ => offerDao.getOffer(offerId))
      .map(OfferResponse)
      .convertError

  def updateOfferStatus(request: UpdateOfferStatusBatchRequest): EitherT[F, ApiError, OkResponse] =
    request.requests
      .traverse { request =>
        offerDao.updateOfferStatus(request.offerId, request.newStatus)
      }
      .as(OkResponse())
      .convertError

  def deleteOffer(userId: UserID, offerId: OfferID): EitherT[F, ApiError, OkResponse] =
    offerDao
      .updateOffer(userId, offerId, OfferPatch(status = Some(OfferStatus.Deleted)))
      .as(OkResponse())
      .convertError

  def addPhotos(userId: UserID, offerId: OfferID, request: AddOfferPhotosRequest): EitherT[F, ApiError, OfferResponse] =
    (for {
      photosU <- EitherT.liftF(request.photoUrls.traverse(createPhoto))
      photosT <- EitherT.liftF(request.telegramIds.traverse(createPhoto))
      photos = photosU ++ photosT
      _ <- offerDao.addPhotos(userId, offerId, photos)
      offer <- offerDao.getOffer(offerId)
    } yield OfferResponse(offer)).convertError

  def deleteAllPhotos(userId: UserID, offerId: OfferID): EitherT[F, ApiError, OkResponse] =
    offerDao
      .deleteAllPhotos(userId, offerId)
      .as(OkResponse())
      .convertError

  def search(query: String, limit: Int): EitherT[F, ApiError, OfferIdsResponse] =
    (for {
      // TODO: should be optimized
      resultsPhraseName <- offerDao.searchPhraseName(query, limit)
      resultsPlainName <- offerDao.searchPlainName(query, limit)
      resultsAnyWordsName <- offerDao.searchAnyWordsName(splitQuery(query), limit)
      resultsPhrase <- offerDao.searchPhrase(query, limit)
      resultsPlain <- offerDao.searchPlain(query, limit)
      resultsAnyWords <- offerDao.searchAnyWords(splitQuery(query), limit)
      resultsAll = List(
        Random.shuffle(resultsPhraseName),
        Random.shuffle(resultsPlainName),
        Random.shuffle(resultsAnyWordsName),
        Random.shuffle(resultsPhrase),
        Random.shuffle(resultsPlain),
        Random.shuffle(resultsAnyWords)
      ).flatten
      results = resultsAll.distinct.take(limit)
    } yield OfferIdsResponse(results)).convertError

  // internal

  private def createPhoto(url: URL): F[Photo] =
    for {
      id <- UUIDGen[F].randomUUID
      photo = Photo(id, Some(url), None, None)
    } yield photo

  private def createPhoto(telegramId: String): F[Photo] =
    for {
      id <- UUIDGen[F].randomUUID
      photo = Photo(id, None, None, Some(telegramId))
    } yield photo

  private def splitQuery(query: String): Seq[String] =
    query.split("\\s+")
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
