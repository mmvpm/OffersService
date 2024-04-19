package com.github.mmvpm.parsing.producer

import cats.data.EitherT

trait PageProducer[F[_]] {
  def run: EitherT[F, String, Unit]
}
