package com.github.mmvpm.service.dao.offer

import cats.Monad
import cats.data.{EitherT, NonEmptyList}
import cats.effect.kernel.MonadCancelThrow
import cats.implicits._
import com.github.mmvpm.model.{Offer, OfferID, UserID}
import com.github.mmvpm.service.dao.error._
import com.github.mmvpm.service.dao.schema.{DoobieSupport, OfferPatch, OffersEntry}
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

  override def getOffer(offerId: OfferID): EitherT[F, OfferDaoError, Offer] =
    selectFromOffers(offerId)
      .map(_.toOffer)
      .transact(tr)
      .attemptT
      .leftMap {
        case UnexpectedEnd => OfferNotFoundDaoError(offerId)
        case error         => InternalOfferDaoError(error.getMessage)
      }

  override def getOffers(offerIds: NonEmptyList[OfferID]): EitherT[F, OfferDaoError, List[Offer]] =
    selectFromOffers(offerIds)
      .map(_.map(_.toOffer))
      .transact(tr)
      .attemptT
      .leftMap(error => InternalOfferDaoError(error.getMessage))

  override def getOffersByUser(userId: UserID): EitherT[F, OfferDaoError, List[Offer]] =
    selectFromOffersByUser(userId)
      .map(_.map(_.toOffer))
      .transact(tr)
      .attemptT
      .leftMap(error => InternalOfferDaoError(error.getMessage))

  override def createOffer(offer: Offer): EitherT[F, OfferDaoError, Unit] =
    Monad[ConnectionIO]
      .map2(insertIntoOffers(offer), insertIntoUserOffers(offer))(_ && _)
      .transact(tr)
      .attemptT
      .handleDefaultErrors

  override def updateOffer(userId: UserID, offerId: OfferID, patch: OfferPatch): EitherT[F, OfferDaoError, Unit] =
    updateOffers(userId, offerId, patch)
      .transact(tr)
      .attemptT
      .handleDefaultErrors

  // queries

  private def selectFromOffers(offerId: OfferID): ConnectionIO[OffersEntry] =
    sql"""
      select id, uo.user_id, name, price, description, status
      from offers
      join user_offers uo on offers.id = uo.offer_id
      where id = $offerId
      """
      .query[OffersEntry]
      .unique

  private def selectFromOffers(offerIds: NonEmptyList[OfferID]): ConnectionIO[List[OffersEntry]] =
    (fr"""
      select id, uo.user_id, name, price, description, status
      from offers
      join user_offers uo on offers.id = uo.offer_id
      where """ ++ in(fr"id", offerIds))
      .query[OffersEntry]
      .to[List]

  private def selectFromOffersByUser(userId: UserID): ConnectionIO[List[OffersEntry]] =
    sql"""
      select id, uo.user_id, name, price, description, status
      from offers
      join user_offers uo on offers.id = uo.offer_id
      where uo.user_id = $userId
      """
      .query[OffersEntry]
      .to[List]

  private def insertIntoOffers(offer: Offer): ConnectionIO[Boolean] = {
    import offer._
    import description._
    sql"insert into offers values ($id, $name, $price, $text, $status)".update.run.map(_ == 1)
  }

  private def insertIntoUserOffers(offer: Offer): ConnectionIO[Boolean] =
    sql"insert into user_offers values (${offer.id}, ${offer.userId})".update.run.map(_ == 1)

  private def updateOffers(userId: UserID, offerId: OfferID, patch: OfferPatch): ConnectionIO[Boolean] =
    (fr"update offers set" ++ sqlByPatch(patch) ++
      fr"from user_offers uo where id = $offerId and uo.user_id = $userId").update.run.map(_ == 1)

  // internal

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
