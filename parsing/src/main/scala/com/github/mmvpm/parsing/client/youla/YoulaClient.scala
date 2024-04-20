package com.github.mmvpm.parsing.client.youla

import cats.data.EitherT
import com.github.mmvpm.parsing.client.youla.response.CatalogResponse

trait YoulaClient[F[_]] {
  def search(query: String, pageNumber: Int): EitherT[F, String, CatalogResponse]
}
