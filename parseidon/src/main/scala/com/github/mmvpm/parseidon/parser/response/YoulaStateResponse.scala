package com.github.mmvpm.parseidon.parser.response

import com.github.mmvpm.parseidon.model.{YoulaOffer, YoulaUser}
import com.github.mmvpm.util.StringUtils.RichString

case class YoulaState(entities: Entities) {

  def toYoulaOffers: List[YoulaOffer] =
    entities.products.map { product =>
      YoulaOffer(
        product.name,
        (product.price / 100).toInt, // source price in kopecks
        product.description,
        product.images.flatMap(_.url.tryToURL)
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
