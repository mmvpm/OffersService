package com.github.mmvpm.parseidon.dao.queue

import cats.data.EitherT
import com.github.mmvpm.parseidon.model.Page

trait PageQueueReader[F[_]] {
  def getNextBlocking: EitherT[F, String, Page]
}
