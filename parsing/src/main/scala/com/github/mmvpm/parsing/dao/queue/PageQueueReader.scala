package com.github.mmvpm.parsing.dao.queue

import cats.data.EitherT
import com.github.mmvpm.parsing.model.Page

trait PageQueueReader[F[_]] {
  def getNextBlocking: EitherT[F, String, Page]
}
