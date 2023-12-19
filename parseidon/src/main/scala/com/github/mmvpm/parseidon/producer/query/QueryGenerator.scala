package com.github.mmvpm.parseidon.producer.query

trait QueryGenerator[F[_]] {
  def randomQuery: F[String]
}
