package com.github.mmvpm.parseidon.dao.visited

import cats.data.EitherT
import com.github.mmvpm.parseidon.model.Page

trait PageVisitedDao[F[_]] {
  def isVisited(page: Page): EitherT[F, String, Boolean]
  def markVisited(page: Page): EitherT[F, String, Unit]
}
