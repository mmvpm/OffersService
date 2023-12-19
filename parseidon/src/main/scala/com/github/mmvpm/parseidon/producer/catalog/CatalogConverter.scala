package com.github.mmvpm.parseidon.producer.catalog

import com.github.mmvpm.parseidon.client.youla.response.CatalogResponse
import com.github.mmvpm.parseidon.model.Page

trait CatalogConverter {
  def convert(catalog: CatalogResponse): List[Page]
}
