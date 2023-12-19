package com.github.mmvpm.nemia.dao.offer

import cats.data.EitherT
import cats.Monad
import cats.effect.Sync
import com.github.mmvpm.nemia.dao.DaoUpdate
import com.github.mmvpm.model.{Offer, OfferID, UserID}
import com.github.mmvpm.nemia.dao.error._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class OfferDaoInMemory[F[_]: Monad: Sync] extends OfferDao[F] {

  override def getOffers(offerIds: List[OfferID]): EitherT[F, OfferDaoError, List[Offer]] =
    EitherT.right {
      Sync[F].delay {
        val offerIdSet = offerIds.toSet
        storage.filter(offer => offerIdSet.contains(offer.id)).toList
      }
    }

  override def getOffersByUser(userId: UserID): EitherT[F, OfferDaoError, List[Offer]] =
    EitherT.right(Sync[F].delay(storage.filter(_.userId == userId).toList))

  override def createOffer(offer: Offer): EitherT[F, OfferDaoError, Unit] =
    for {
      _ <- EitherT.cond(!storage.exists(_.id == offer.id), (), OfferAlreadyExistsDaoError(offer.id))
      _ <- EitherT.liftF(Sync[F].delay(storage.append(offer)))
    } yield ()

  override def updateOffer(offerId: OfferID, updateFunc: Offer => DaoUpdate[Offer]): EitherT[F, OfferDaoError, Offer] =
    for {
      offer <- EitherT.fromOption(storage.find(_.id == offerId), OfferNotFoundDaoError(offerId))
      newOffer <- EitherT.liftF {
        updateFunc(offer) match {
          case DaoUpdate.DoNothing => Monad[F].pure(offer)
          case DaoUpdate.SaveNew(newOffer) => Sync[F].delay {
            storage.update(storage.indexOf(offer), newOffer)
            newOffer
          }
        }
      }
    } yield newOffer

  private val storage: mutable.Buffer[Offer] = new ArrayBuffer()
}
