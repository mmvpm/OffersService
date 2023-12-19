package com.github.mmvpm.nemia.api

import sttp.tapir.server.ServerEndpoint

trait Handler[F[_]] {
  def endpoints: List[ServerEndpoint[Any, F]]
}
