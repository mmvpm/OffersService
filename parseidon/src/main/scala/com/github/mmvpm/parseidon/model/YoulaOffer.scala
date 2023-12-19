package com.github.mmvpm.parseidon.model

import com.github.mmvpm.model.{Money, OfferDescription, Photo}

import java.net.URL

case class YoulaOffer(name: String, price: Money, text: String, photoUrls: List[URL]) {

  def toOffer: OfferDescription =
    OfferDescription(name, price, text, photos = photoUrls.map(Photo))
}
