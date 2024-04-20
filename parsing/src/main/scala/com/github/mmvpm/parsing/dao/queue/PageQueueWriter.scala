package com.github.mmvpm.parsing.dao.queue

import cats.data.EitherT
import com.github.mmvpm.parsing.model.Page

trait PageQueueWriter[F[_]] {
  def append(page: Page): EitherT[F, String, Unit]
}
