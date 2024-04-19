package com.github.mmvpm.parsing.producer.catalog

import com.github.mmvpm.parsing.client.youla.response.CatalogResponse
import com.github.mmvpm.parsing.model.Page
import com.github.mmvpm.parsing.YoulaConfig
import com.github.mmvpm.parsing.util.StringUtils.StringSyntax

class CatalogConverterImpl(youlaConfig: YoulaConfig) extends CatalogConverter {

  override def convert(catalog: CatalogResponse): List[Page] =
    catalog.data.feed.items.flatMap(_.product).flatMap { product =>
      val suffix = product.url.stripPrefix("/")
      val url = s"${youlaConfig.baseUrl}/$suffix".toURLOption
      url.map(Page)
    }
}
