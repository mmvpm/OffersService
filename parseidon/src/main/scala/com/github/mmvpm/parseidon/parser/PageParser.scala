package com.github.mmvpm.parseidon.parser

import cats.data.EitherT
import com.github.mmvpm.parseidon.model.{Page, YoulaItem}

trait PageParser[F[_]] {
  def parse(page: Page): EitherT[F, String, List[YoulaItem]]
}
