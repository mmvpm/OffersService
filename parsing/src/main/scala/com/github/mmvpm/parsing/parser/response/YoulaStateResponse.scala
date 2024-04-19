package com.github.mmvpm.parsing.parser.response

import com.github.mmvpm.parsing.model.{YoulaOffer, YoulaUser}
import com.github.mmvpm.parsing.util.StringUtils.StringSyntax

case class YoulaState(entities: Entities) {

  def toYoulaOffers: List[YoulaOffer] =
    entities.products.map { product =>
      YoulaOffer(
        product.name,
        (product.price / 100).toInt, // source price in kopecks
        product.description,
        product.images.flatMap(_.url.toURLOption)
      )
    }

  def toYoulaUsers: List[YoulaUser] =
    entities.products.map { product =>
      YoulaUser(
        product.contractor.id,
        product.contractor.name
      )
    }
}

case class Entities(products: List[Product])

case class Product(name: String, description: String, price: Long, images: List[Image], contractor: Contractor)

case class Image(url: String)

case class Contractor(id: String, name: String)
