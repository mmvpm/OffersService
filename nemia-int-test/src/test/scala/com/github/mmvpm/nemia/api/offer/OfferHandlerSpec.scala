package com.github.mmvpm.nemia.api.offer

import cats.effect.{IO, Resource}
import cats.effect.std.Random
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.github.mmvpm.nemia.api.{AuthHandler, OfferHandler}
import com.github.mmvpm.nemia.api.error.ApiError
import com.github.mmvpm.nemia.api.util.{ConfigSupport, PostgresContainerSupport, RedisContainerSupport}
import com.github.mmvpm.nemia.api.util.request.{AuthRequestsSupport, OfferRequestsSupport}
import com.github.mmvpm.nemia.dao.offer.OfferDaoPostgresql
import com.github.mmvpm.nemia.dao.session.SessionDaoRedis
import com.github.mmvpm.nemia.dao.user.UserDaoPostgresql
import com.github.mmvpm.nemia.service.auth.AuthServiceImpl
import com.github.mmvpm.nemia.service.offer.OfferServiceImpl
import com.github.mmvpm.nemia.service.user.UserServiceImpl
import com.github.mmvpm.util.Logging
import com.redis.RedisClient
import doobie.Transactor
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.FixtureAsyncWordSpec
import sttp.client3.SttpBackend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.integ.cats.effect.CatsMonadError
import sttp.tapir.server.stub.TapirStubInterpreter

class OfferHandlerSpec
  extends FixtureAsyncWordSpec
    with AsyncIOSpec
    with CatsResourceIO[Transactor[IO]]
    with Matchers
    with ConfigSupport
    with RedisContainerSupport
    with PostgresContainerSupport
    with OfferRequestsSupport
    with Logging {

  "OfferHandler" should {
    "create a new user" in { implicit p =>
      for {
        backend <- createBackend
        redis = new RedisClient(config.redis.host, config.redis.port)
        a = redis.set("a", "b")
        _ = log.info(s"### [redis set]: $a")
      } yield ()
    }

  }

  private def show[T](r: Either[ApiError, T]): String =
    r match {
      case Left(value) => s"ApiError(${value.id}, ${value.code}, ${value.details})"
      case Right(value) => value.toString
    }

  override val resource: Resource[IO, Transactor[IO]] =
    for {
      redis <- makeRedisContainer
      _ = log.info(s"### redis uri = ${redis.getRedisURI}")
      tr <- makePostgresTransactor
    } yield tr

  private def createBackend(implicit tr: Transactor[IO]): IO[SttpBackend[IO, Any]] =
    for {
      _ <- IO.pure(())
      redis = new RedisClient(config.redis.host, config.redis.port)

      sessionDao = new SessionDaoRedis[IO](redis, config.session.expiration.toSeconds)
      userDao = new UserDaoPostgresql[IO]()
      offerDao = new OfferDaoPostgresql[IO]()

      authService = new AuthServiceImpl[IO](userDao, sessionDao, config.session.expiration.toSeconds)
      offerService = new OfferServiceImpl[IO](offerDao)

      handler = new OfferHandler[IO](offerService, authService)

      stub = SttpBackendStub(new CatsMonadError[IO]())
      sttpBackend = TapirStubInterpreter(stub)
        .whenServerEndpointsRunLogic(handler.endpoints)
        .backend()
    } yield sttpBackend
}
