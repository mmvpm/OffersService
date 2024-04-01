package com.github.mmvpm.bot.model

import com.github.mmvpm.bot.client.ofs.request.UpdateOfferRequest
import com.github.mmvpm.model.OfferDescription

case class OfferPatch(
    name: Option[String] = None,
    price: Option[Int] = None,
    description: Option[String] = None
) {
  def toUpdateOfferRequest: UpdateOfferRequest =
    UpdateOfferRequest(name, price, description)
}
