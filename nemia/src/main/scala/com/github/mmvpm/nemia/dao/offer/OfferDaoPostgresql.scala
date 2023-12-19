package com.github.mmvpm.nemia.dao.offer

import cats.data.EitherT
import cats.effect.kernel.MonadCancelThrow
import cats.free.Free
import cats.implicits.catsSyntaxApplicativeError
import cats.Monad
import com.github.mmvpm.nemia.dao.{DaoUpdate, QuillSupport}
import com.github.mmvpm.nemia.dao.table.{OfferPhotos, Offers}
import com.github.mmvpm.nemia.dao.DaoUpdate._
import com.github.mmvpm.nemia.dao.util.DbSyntax.RichOffers
import com.github.mmvpm.model.{Offer, OfferID, UserID}
import com.github.mmvpm.nemia.dao.error._
import com.github.mmvpm.nemia.dao.util.ThrowableUtils.DuplicateKeyException
import com.github.mmvpm.util.Logging
import com.github.mmvpm.util.MonadUtils.{ensure, EnsureException}
import doobie.free.connection.ConnectionOp
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.ConnectionIO
import io.getquill.doobie.DoobieContext
import io.getquill.SnakeCase
import io.getquill.mirrorContextWithQueryProbing.transaction

class OfferDaoPostgresql[F[_]: MonadCancelThrow: Monad](implicit val tr: Transactor[F])
  extends OfferDao[F]
  with QuillSupport
  with Logging {

  private val ctx = new DoobieContext.Postgres(SnakeCase)
  import ctx._

  override def getOffers(offerIds: List[OfferID]): EitherT[F, OfferDaoError, List[Offer]] = transaction {
    for {
      dbOffers <- run(query[Offers].filter(offer => liftQuery(offerIds).contains(offer.id)))
      dbPhotos <- run(query[OfferPhotos].filter(photos => liftQuery(offerIds).contains(photos.offerId)))
    } yield assembleOffers(dbOffers, dbPhotos)
  }.transact(tr).attemptT.leftMap { t: Throwable =>
    log.error(s"get offers $offerIds failed", t)
    InternalOfferDaoError(t.getMessage)
  }

  override def getOffersByUser(userId: UserID): EitherT[F, OfferDaoError, List[Offer]] = transaction {
    for {
      dbOffers <- run(query[Offers].filter(_.userId == lift(userId)))
      dbPhotos <- run(query[OfferPhotos].filter(photos => liftQuery(dbOffers.map(_.id)).contains(photos.offerId)))
    } yield assembleOffers(dbOffers, dbPhotos)
  }.transact(tr).attemptT.leftMap { t: Throwable =>
    log.error(s"get offers by user $userId failed", t)
    InternalOfferDaoError(t.getMessage)
  }

  override def createOffer(offer: Offer): EitherT[F, OfferDaoError, Unit] =
    insert(Offers.from(offer), OfferPhotos.from(offer)).attemptT.leftMap {
      case e: EnsureException =>
        log.error(s"create offer ${offer.id} failed", e)
        OfferInsertFailedDaoError(offer.id)
      case DuplicateKeyException(_) =>
        OfferAlreadyExistsDaoError(offer.id)
      case t: Throwable =>
        InternalOfferDaoError(t.getMessage)
    }

  override def updateOffer(offerId: OfferID, updateFunc: Offer => DaoUpdate[Offer]): EitherT[F, OfferDaoError, Offer] =
    update(offerId, updateFunc).attemptT.leftMap {
      case e: EnsureException =>
        log.error(s"update offer $offerId failed", e)
        OfferUpdateFailedDaoError(offerId)
      case t: Throwable =>
        InternalOfferDaoError(t.getMessage)
    }

  private def insert(offers: Offers, offerPhotos: List[OfferPhotos]): F[Unit] = transaction {
    for {
      r1 <- run(query[Offers].insertValue(lift(offers)))
      r2 <- run(liftQuery(offerPhotos).foreach(query[OfferPhotos].insertValue(_)))
      _ = ensure[F](r1 == 1 && r2.forall(_ == 1), s"$r1 == 1 && $r2.forall(_ == 1)")
    } yield ()
  }.transact(tr)

  private def update(offerId: OfferID, updateFunc: Offer => DaoUpdate[Offer]): F[Offer] = transaction {
    for {
      dbOffer <- run(query[Offers].filter(_.id == lift(offerId)))
      dbOfferPhotos <- run(query[OfferPhotos].filter(_.offerId == lift(offerId)))
      offer = assembleOffer(dbOffer.single, dbOfferPhotos)
      updatedOffer <- updateFunc(offer) match {
        case DoNothing => Free.pure[ConnectionOp, Offer](offer)
        case SaveNew(newOffer) => updateRaw(offerId, newOffer)
      }
    } yield updatedOffer
  }.transact(tr)

  private def updateRaw(offerId: OfferID, newOffer: Offer): ConnectionIO[Offer] =
    for {
      r1 <- run {
        query[Offers]
          .filter(_.id == lift(offerId))
          .updateValue(lift(Offers.from(newOffer)))
      }
      r2 <- run {
        query[OfferPhotos]
          .filter(_.offerId == lift(offerId))
          .delete
      }
      r3 <- run {
        liftQuery(OfferPhotos.from(newOffer)).foreach { photos =>
          query[OfferPhotos].insertValue(photos)
        }
      }
      _ = ensure[F](r1 == 1 && r2 == 1 && r3.forall(_ == 1), s"$r1 == 1 && $r2 == 1 && $r3.forall(_ == 1)")
    } yield newOffer

  private def assembleOffers(dbOffers: List[Offers], dbPhotos: List[OfferPhotos]): List[Offer] = {
    val photosMap = dbPhotos.groupBy(_.offerId)
    dbOffers.map { offer =>
      assembleOffer(offer, photosMap.getOrElse(offer.id, List.empty))
    }
  }

  private def assembleOffer(dbOffer: Offers, dbPhotos: List[OfferPhotos]): Offer =
    OfferPhotos.mergeTo(dbOffer.toOffer, dbPhotos)
}
