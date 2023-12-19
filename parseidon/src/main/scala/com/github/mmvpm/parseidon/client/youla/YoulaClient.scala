package com.github.mmvpm.parseidon.client.youla

import cats.data.EitherT
import com.github.mmvpm.parseidon.client.youla.response.CatalogResponse
import com.github.mmvpm.parseidon.model.Page

trait YoulaClient[F[_]] {
  def search(query: String): EitherT[F, String, CatalogResponse]
}
