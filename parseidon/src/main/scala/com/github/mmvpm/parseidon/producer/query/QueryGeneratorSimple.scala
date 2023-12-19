package com.github.mmvpm.parseidon.producer.query

import cats.Applicative

class QueryGeneratorSimple[F[_]: Applicative] extends QueryGenerator[F] {
  override def randomQuery: F[String] = Applicative[F].pure("собака")
}
