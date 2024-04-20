package com.github.mmvpm.parsing.client.youla.response

case class CatalogResponse(data: Data)

case class Data(feed: Feed)

case class Feed(items: List[Item])

case class Item(product: Option[Product])

case class Product(url: String)
