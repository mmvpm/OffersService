package com.github.mmvpm.parsing.producer.query

trait QueryGenerator[F[_]] {
  def randomQuery: F[String]
}
