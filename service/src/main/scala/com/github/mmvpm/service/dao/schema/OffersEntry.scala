package com.github.mmvpm.service.dao.schema

import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model._

case class OffersEntry(
    id: OfferID,
    userId: UserID,
    name: String,
    price: Int,
    text: String,
    status: OfferStatus,
    source: Option[String]
) {

  def toOffer: Offer =
    Offer(id, userId, OfferDescription(name, price, text), status, source)
}

object OffersEntry {

  def from(offer: Offer): OffersEntry =
    OffersEntry(
      offer.id,
      offer.userId,
      offer.description.name,
      offer.description.price,
      offer.description.text,
      offer.status,
      offer.source
    )
}
