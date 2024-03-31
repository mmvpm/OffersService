package com.github.mmvpm.service

import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.std.Random
import com.comcast.ip4s.{Host, Port}
import com.github.mmvpm.service.api.{AuthHandler, OfferHandler, UserHandler}
import com.github.mmvpm.service.dao.offer.{OfferDao, OfferDaoPostgresql}
import com.github.mmvpm.service.dao.session.{SessionDao, SessionDaoInMemory, SessionDaoRedis}
import com.github.mmvpm.service.dao.user.{UserDao, UserDaoPostgresql}
import com.github.mmvpm.service.dao.util.FlywayMigration
import com.github.mmvpm.service.dao.util.Postgresql.makeTransactor
import com.github.mmvpm.service.service.auth.{AuthService, AuthServiceImpl}
import com.github.mmvpm.service.service.offer.{OfferService, OfferServiceImpl}
import com.github.mmvpm.service.service.user.{UserService, UserServiceImpl}
import com.redis.RedisClient
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
      random <- Random.scalaUtilRandom[IO]
      redis = new RedisClient(config.redis.host, config.redis.port)

      offerDao: OfferDao[IO] = new OfferDaoPostgresql[IO]
      sessionDao: SessionDao[IO] = new SessionDaoRedis[IO](redis, config.session.expiration.toSeconds)
      userDao: UserDao[IO] = new UserDaoPostgresql[IO]

      authService: AuthService[IO] = new AuthServiceImpl[IO](userDao, sessionDao)
      offerService: OfferService[IO] = new OfferServiceImpl[IO](offerDao)
      userService: UserService[IO] = new UserServiceImpl[IO](userDao, random)
      authHandler: AuthHandler[IO] = new AuthHandler(authService, userService)
      offerHandler: OfferHandler[IO] = new OfferHandler[IO](offerService, authService)
      userHandler: UserHandler[IO] = new UserHandler[IO](userService, authService)

      handlers = List(authHandler, offerHandler, userHandler)
      endpoints <- IO.delay(handlers.flatMap(_.endpoints))
      routes = Http4sServerInterpreter[IO]().toRoutes(swaggerBy(endpoints) ++ endpoints)
      server <- serverBuilder(config, routes).leftMap(new IllegalArgumentException(_)).value.rethrow

      _ <- FlywayMigration.migrate[IO](config.postgresql)

      _ <- server.build.use { server =>
        for {
          _ <- IO.println(s"SwaggerUI: http://${server.address.getHostName}:${server.address.getPort}/docs")
          _ <- IO.readLine
        } yield ()
      }
    } yield ExitCode.Success

  private def serverBuilder(config: Config, routes: HttpRoutes[IO]): EitherT[IO, String, EmberServerBuilder[IO]] =
    for {
      host <- EitherT.fromOption[IO](Host.fromString(config.server.host), s"incorrect host '${config.server.host}'")
      port <- EitherT.fromOption[IO](Port.fromInt(config.server.port), s"incorrect port '${config.server.port}'")
      builder <- EitherT.liftF {
        IO.delay {
          EmberServerBuilder.default[IO].withHost(host).withPort(port).withHttpApp(Router("/" -> routes).orNotFound)
        }
      }
    } yield builder

  private def swaggerBy[A](endpoints: List[ServerEndpoint[A, IO]]): List[ServerEndpoint[A, IO]] =
    SwaggerInterpreter().fromServerEndpoints[IO](endpoints, "offers-service", "1.0.0")
}
