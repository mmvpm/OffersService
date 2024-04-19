package com.github.mmvpm.parsing.consumer

import cats.data.EitherT

trait PageConsumer[F[_]] {
  def run: EitherT[F, String, Unit]
}
