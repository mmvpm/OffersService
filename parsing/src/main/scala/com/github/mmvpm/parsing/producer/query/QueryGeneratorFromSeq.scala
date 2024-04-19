package com.github.mmvpm.parsing.producer.query

import cats.effect.std.Random
import cats.MonadThrow

class QueryGeneratorFromSeq[F[_]: MonadThrow](words: Seq[String], random: Random[F]) extends QueryGenerator[F] {

  override def randomQuery: F[String] =
    random.elementOf(words)
}
