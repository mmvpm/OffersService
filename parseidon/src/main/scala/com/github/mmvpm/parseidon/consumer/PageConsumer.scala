package com.github.mmvpm.parseidon.consumer

import cats.data.EitherT

trait PageConsumer[F[_]] {
  def run: EitherT[F, String, Unit]
}
