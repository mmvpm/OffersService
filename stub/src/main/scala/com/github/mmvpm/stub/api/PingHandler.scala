package com.github.mmvpm.stub.api
import cats.Applicative
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

class PingHandler[F[_]: Applicative] extends Handler[F] {

  private val ping: ServerEndpoint[Any, F] =
    endpoint.get
      .summary("Ping")
      .in("api" / "v1" / "ping")
      .out(stringBody)
      .serverLogic(_ => Applicative[F].pure(Right("pong")))

  override def endpoints: List[ServerEndpoint[Any, F]] =
    List(ping).map(_.withTag("util"))
}
