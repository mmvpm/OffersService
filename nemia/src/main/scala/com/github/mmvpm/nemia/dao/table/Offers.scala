package com.github.mmvpm.nemia.dao.table

import com.github.mmvpm.model._
import com.github.mmvpm.model.OfferStatus.OfferStatus

import java.time.Instant
import java.util.UUID

case class Offers(
    id: UUID,
    userId: UUID,
    name: String,
    price: Int,
    text: String,
    status: OfferStatus,
    createdAt: Instant,
    updatedAt: Instant) {

  def toOffer: Offer =
    Offer(id, userId, OfferDescription(name, price, text, List()), status, createdAt, updatedAt)
}

object Offers {

  def from(offer: Offer): Offers =
    Offers(
      offer.id,
      offer.userId,
      offer.description.name,
      offer.description.price,
      offer.description.text,
      offer.status,
      offer.createdAt,
      offer.updatedAt
    )
}
