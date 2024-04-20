package com.github.mmvpm.parsing.producer.catalog

import com.github.mmvpm.parsing.client.youla.response.CatalogResponse
import com.github.mmvpm.parsing.model.Page

trait CatalogConverter {
  def convert(catalog: CatalogResponse): List[Page]
}
