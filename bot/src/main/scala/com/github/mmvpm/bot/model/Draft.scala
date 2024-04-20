package com.github.mmvpm.bot.model

import com.github.mmvpm.model.OfferDescription

case class Draft(
    name: Option[String] = None,
    price: Option[Int] = None,
    description: Option[String] = None,
    photos: Seq[TgPhoto] = Seq.empty
) {

  def toOfferDescription: Option[OfferDescription] =
    for {
      name <- name
      price <- price
      description <- description
    } yield OfferDescription(name, price, description)
}
