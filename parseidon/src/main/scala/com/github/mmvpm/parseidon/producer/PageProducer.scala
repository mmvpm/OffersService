package com.github.mmvpm.parseidon.producer

import cats.data.EitherT

trait PageProducer[F[_]] {
  def run: EitherT[F, String, Unit]
}
