package com.github.mmvpm.parseidon.producer.catalog

import com.github.mmvpm.parseidon.client.youla.response.CatalogResponse
import com.github.mmvpm.parseidon.model.Page
import com.github.mmvpm.parseidon.YoulaConfig
import com.github.mmvpm.util.StringUtils.RichString

class CatalogConverterImpl(youlaConfig: YoulaConfig) extends CatalogConverter {

  override def convert(catalog: CatalogResponse): List[Page] =
    catalog.data.feed.items.flatMap(_.product).flatMap { product =>
      val suffix = product.url.stripPrefix("/")
      val url = s"${youlaConfig.baseUrl}/$suffix".tryToURL
      url.map(Page)
    }
}
