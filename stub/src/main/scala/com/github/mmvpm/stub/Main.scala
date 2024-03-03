package com.github.mmvpm.stub

import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.catsSyntaxApplicativeError
import com.comcast.ip4s.{Host, Port}
import com.github.mmvpm.stub.api._
import com.github.mmvpm.stub.dao._
import com.github.mmvpm.stub.service._
import com.github.mmvpm.stub.util.FlywayMigration
import com.github.mmvpm.stub.util.Postgresql.makeTransactor
import doobie.Transactor
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.HttpRoutes
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigSource.default.loadOrThrow[Config]
    makeTransactor[IO](config.postgresql).use(runServer(config)(_))
  }

  private def runServer(config: Config)(implicit xa: Transactor[IO]): IO[ExitCode] =
    for {
      _ <- IO.pure(0)

      stubDao: StubDao[IO] = new StubDaoPostgresql[IO]
      stubService: StubService[IO] = new StubServiceImpl[IO](stubDao)
      stubHandler: StubHandler[IO] = new StubHandler[IO](stubService)

      handlers = List(stubHandler)
      endpoints <- IO.delay(handlers.flatMap(_.endpoints))
      routes = Http4sServerInterpreter[IO].toRoutes(swaggerBy(endpoints) ++ endpoints)
      server <- serverBuilder(config, routes).value.rethrow

      _ <- FlywayMigration.migrate[IO](config.postgresql)

      _ <- server.build.use { server =>
        val (host, port) = (server.address.getHostName, server.address.getPort)
        IO.println(s"SwaggerUI: http://$host:$port/docs") >> IO.never
      }
    } yield ExitCode.Success

  private def serverBuilder(config: Config, routes: HttpRoutes[IO]): EitherT[IO, Throwable, EmberServerBuilder[IO]] =
    for {
      host <- parseHost(config.server.host)
      port <- parsePort(config.server.port)
      builder <- IO.delay {
        EmberServerBuilder.default[IO]
          .withHost(host)
          .withPort(port)
          .withHttpApp(
            Router("/" -> routes)
              .orNotFound
          )
      }.attemptT
    } yield builder

  private def parseHost(host: String): EitherT[IO, Throwable, Host] =
    EitherT.fromOption[IO](Host.fromString(host), new IllegalArgumentException(s"incorrect host '$host'"))

  private def parsePort(port: Int): EitherT[IO, Throwable, Port] =
    EitherT.fromOption[IO](Port.fromInt(port), new IllegalArgumentException(s"incorrect port '$port'"))

  private def swaggerBy[A](endpoints: List[ServerEndpoint[A, IO]]): List[ServerEndpoint[A, IO]] =
    SwaggerInterpreter().fromServerEndpoints[IO](endpoints, "offers-service", "1.0.0")
}
