package com.github.mmvpm.service.dao.offer

import cats.Monad
import cats.data.{EitherT, NonEmptyList}
import cats.effect.kernel.MonadCancelThrow
import cats.implicits._
import com.github.mmvpm.model.{Offer, OfferID, Photo, PhotoID, UserID}
import com.github.mmvpm.service.dao.error._
import com.github.mmvpm.service.dao.schema.{DoobieSupport, OfferPatch, OffersEntry, PhotosEntry, UserOffersEntry}
import com.github.mmvpm.util.Logging
import doobie.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragment.Fragment
import doobie.util.fragments.in
import doobie.util.invariant.UnexpectedEnd
import doobie.util.transactor.Transactor

class OfferDaoPostgresql[F[_]: MonadCancelThrow](implicit val tr: Transactor[F])
    extends OfferDao[F]
    with DoobieSupport
    with Logging {

  def getOffer(offerId: OfferID): EitherT[F, OfferDaoError, Offer] =
    (for {
      offersEntry <- selectFromOffers(offerId)
      photosEntries <- selectFromPhotos(offerId)
      offer = offersEntry.toOffer(photosEntries)
    } yield offer)
      .transact(tr)
      .attemptT
      .leftMap {
        case UnexpectedEnd => OfferNotFoundDaoError(offerId)
        case error         => InternalOfferDaoError(error.getMessage)
      }

  def getOffers(offerIds: List[OfferID]): EitherT[F, OfferDaoError, List[Offer]] =
    NonEmptyList.fromList(offerIds) match {
      case None =>
        EitherT.pure(List.empty[Offer])
      case Some(nel) =>
        (for {
          offersEntries <- selectFromOffers(nel)
          offers <- offersEntries.traverse { offersEntry =>
            selectFromPhotos(offersEntry.id).map(offersEntry.toOffer)
          }
        } yield offers)
          .transact(tr)
          .attemptT
          .leftMap(error => InternalOfferDaoError(error.getMessage))
    }

  def getOffersByUser(userId: UserID): EitherT[F, OfferDaoError, List[Offer]] =
    (for {
      offersEntries <- selectFromOffersByUser(userId)
      offers <- offersEntries.traverse { offersEntry =>
        selectFromPhotos(offersEntry.id).map(offersEntry.toOffer)
      }
    } yield offers)
      .transact(tr)
      .attemptT
      .leftMap(error => InternalOfferDaoError(error.getMessage))

  def createOffer(offer: Offer): EitherT[F, OfferDaoError, Unit] =
    insertOfferToAllTables(offer)
      .transact(tr)
      .attemptT
      .handleDefaultErrors

  def updateOffer(userId: UserID, offerId: OfferID, patch: OfferPatch): EitherT[F, OfferDaoError, Unit] =
    updateOffers(userId, offerId, patch)
      .transact(tr)
      .attemptT
      .handleDefaultErrors

  def addPhotos(userId: UserID, offerId: OfferID, photos: Seq[Photo]): EitherT[F, OfferDaoError, Unit] =
    checkOfferBelonging(userId, offerId)
      .flatMap(_ => insertPhotosToAllTables(offerId, photos))
      .transact(tr)
      .attemptT
      .leftMap {
        case UnexpectedEnd => OfferNotFoundDaoError(offerId)
        case error         => InternalOfferDaoError(error.getMessage)
      }
      .flatMap { insertedPhotos =>
        val success = insertedPhotos == photos.size
        EitherT.cond(success, (), InternalOfferDaoError(s"only $insertedPhotos/${photos.size} photos was inserted"))
      }

  def deleteAllPhotos(userId: UserID, offerId: OfferID): EitherT[F, OfferDaoError, Unit] =
    checkOfferBelonging(userId, offerId)
      .flatMap(_ => deleteFromPhotos(offerId))
      .transact(tr)
      .attemptT
      .void
      .leftMap {
        case UnexpectedEnd => OfferNotFoundDaoError(offerId)
        case error         => InternalOfferDaoError(error.getMessage)
      }

  // internal

  private def insertOfferToAllTables(offer: Offer): ConnectionIO[Boolean] =
    Monad[ConnectionIO].map3(
      insertIntoOffers(offer),
      insertIntoUserOffers(offer),
      insertPhotosToAllTables(offer.id, offer.photos).map(_ == offer.photos.size)
    )(_ && _ && _)

  private def insertPhotosToAllTables(offerId: OfferID, photos: Seq[Photo]): ConnectionIO[Int] =
    photos
      .traverse(insertPhotoToAllTables(offerId, _))
      .map(_.count(_ == true))

  private def insertPhotoToAllTables(offerId: OfferID, photo: Photo): ConnectionIO[Boolean] =
    Monad[ConnectionIO].map2(
      insertIntoPhotos(photo),
      insertIntoOfferPhotos(offerId, photo.id)
    )(_ && _)

  // queries

  private def selectFromOffers(offerId: OfferID): ConnectionIO[OffersEntry] =
    sql"""
      select id, uo.user_id, name, price, description, status, source
      from offers
      join user_offers uo on offers.id = uo.offer_id
      where id = $offerId
      """
      .query[OffersEntry]
      .unique

  private def selectFromOffers(offerIds: NonEmptyList[OfferID]): ConnectionIO[List[OffersEntry]] =
    (fr"""
      select id, uo.user_id, name, price, description, status, source
      from offers
      join user_offers uo on offers.id = uo.offer_id
      where """ ++ in(fr"id", offerIds))
      .query[OffersEntry]
      .to[List]

  private def checkOfferBelonging(userId: UserID, offerId: OfferID): ConnectionIO[Unit] =
    sql"select offer_id, user_id from user_offers where user_id = $userId and offer_id = $offerId"
      .query[UserOffersEntry]
      .unique // throws UnexpectedEnd
      .void

  private def selectFromOffersByUser(userId: UserID): ConnectionIO[List[OffersEntry]] =
    sql"""
      select id, uo.user_id, name, price, description, status, source
      from offers
      join user_offers uo on offers.id = uo.offer_id
      where uo.user_id = $userId
      """
      .query[OffersEntry]
      .to[List]

  private def insertIntoOffers(offer: Offer): ConnectionIO[Boolean] = {
    import offer._
    import description._
    sql"insert into offers values ($id, $name, $price, $text, $status, $source)".update.run.map(_ == 1)
  }

  private def insertIntoUserOffers(offer: Offer): ConnectionIO[Boolean] =
    sql"insert into user_offers values (${offer.id}, ${offer.userId})".update.run.map(_ == 1)

  private def updateOffers(userId: UserID, offerId: OfferID, patch: OfferPatch): ConnectionIO[Boolean] =
    (fr"update offers set" ++ sqlByPatch(patch) ++
      fr"from user_offers uo where id = $offerId and uo.user_id = $userId").update.run.map(_ == 1)

  // photos

  private def selectFromPhotos(offerId: OfferID): ConnectionIO[List[PhotosEntry]] =
    sql"""
      select id, url, blob
      from photos
      join offer_photos op on photos.id = op.photo_id
      where offer_id = $offerId
      """
      .query[PhotosEntry]
      .to[List]

  private def insertIntoPhotos(photo: Photo): ConnectionIO[Boolean] =
    sql"insert into photos values (${photo.id}, ${photo.url}, ${photo.blob})".update.run.map(_ == 1)

  private def insertIntoOfferPhotos(offerId: OfferID, photoId: PhotoID): ConnectionIO[Boolean] =
    sql"insert into offer_photos values ($photoId, $offerId)".update.run.map(_ == 1)

  private def deleteFromPhotos(offerId: OfferID): ConnectionIO[Int] =
    sql"""
      with deleted_photo_ids as (
        delete
        from offer_photos
        where offer_id = $offerId
        returning photo_id
      )
      delete
      from photos
      where id in (select photo_id from deleted_photo_ids);
       """.update.run

  // utils

  private def sqlByPatch(patch: OfferPatch): Fragment = {
    val nameSql = patch.name.map(name => fr"name = $name")
    val descriptionSql = patch.description.map(text => fr"description = $text")
    val priceSql = patch.price.map(price => fr"price = $price")
    val statusSql = patch.status.map(status => fr"status = $status")
    List(nameSql, descriptionSql, priceSql, statusSql).flatten.reduce(_ ++ fr", " ++ _)
  }

  implicit class RichDbUpdate(result: EitherT[F, Throwable, Boolean]) {
    def handleDefaultErrors: EitherT[F, OfferDaoError, Unit] =
      result.biflatMap(
        err => EitherT.leftT[F, Unit](InternalOfferDaoError(err.getMessage)),
        res => EitherT.cond(res, (), InternalOfferDaoError("no rows was updated"))
      )
  }
}
