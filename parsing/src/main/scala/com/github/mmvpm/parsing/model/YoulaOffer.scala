package com.github.mmvpm.parsing.model

import com.github.mmvpm.model.{Money, OfferDescription}

import java.net.URL

case class YoulaOffer(name: String, price: Money, text: String, photoUrls: List[URL], source: URL) {

  def toOffer: OfferDescription =
    OfferDescription(name, price, text)
}
