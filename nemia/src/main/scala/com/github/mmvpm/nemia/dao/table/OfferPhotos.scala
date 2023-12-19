package com.github.mmvpm.nemia.dao.table

import com.github.mmvpm.model.{Offer, OfferID, Photo}

import java.net.URL

case class OfferPhotos(
    offerId: OfferID,
    photoUrl: URL)

object OfferPhotos {

  def from(offer: Offer): List[OfferPhotos] =
    for {
      photoUrl <- offer.description.photos.map(_.url)
    } yield OfferPhotos(offer.id, photoUrl)

  def mergeTo(offer: Offer, offerPhotos: List[OfferPhotos]): Offer = {
    val photos = offerPhotos.map(p => Photo(p.photoUrl))
    offer.copy(description = offer.description.copy(photos = photos))
  }
}
