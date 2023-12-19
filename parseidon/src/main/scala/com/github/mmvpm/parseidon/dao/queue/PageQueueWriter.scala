package com.github.mmvpm.parseidon.dao.queue

import cats.data.EitherT
import com.github.mmvpm.parseidon.model.Page

trait PageQueueWriter[F[_]] {
  def append(page: Page): EitherT[F, String, Unit]
}
