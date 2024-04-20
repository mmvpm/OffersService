package com.github.mmvpm.parsing.parser

import cats.data.EitherT
import com.github.mmvpm.parsing.model.{Page, YoulaItem}

trait PageParser[F[_]] {
  def parse(page: Page): EitherT[F, String, List[YoulaItem]]
}
