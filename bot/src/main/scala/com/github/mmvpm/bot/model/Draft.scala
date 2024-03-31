package com.github.mmvpm.bot.model

import com.github.mmvpm.model.OfferDescription

case class Draft(
    name: Option[String] = None,
    price: Option[Long] = None,
    description: Option[String] = None,
    photos: Seq[String] = Seq.empty
) {

  def toOfferDescription: Option[OfferDescription] =
    for {
      name <- name
      price <- price
      description <- description
    } yield OfferDescription(name, price.toInt, description)
}
