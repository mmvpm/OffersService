package com.github.mmvpm.nemia.api.util

import cats.effect.{IO, Resource}
import cats.effect.std.Random
import com.github.mmvpm.nemia.api.{AuthHandler, OfferHandler, UserHandler}
import com.github.mmvpm.nemia.dao.offer.OfferDaoPostgresql
import com.github.mmvpm.nemia.dao.session.SessionDaoRedis
import com.github.mmvpm.nemia.dao.user.UserDaoPostgresql
import com.github.mmvpm.nemia.service.auth.AuthServiceImpl
import com.github.mmvpm.nemia.service.offer.OfferServiceImpl
import com.github.mmvpm.nemia.service.user.UserServiceImpl
import com.redis.RedisClient
import doobie.Transactor
import sttp.client3.SttpBackend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.integ.cats.effect.CatsMonadError
import sttp.tapir.server.stub.TapirStubInterpreter

import java.net.URI

trait SttpBackendSupport extends ConfigSupport with RedisContainerSupport with PostgresContainerSupport {

  val backend: Resource[IO, SttpBackend[IO, Any]] = {
    for {
      redis <- makeRedisContainer
      tr <- makePostgresTransactor
      backend <- createBackend(redis.getRedisURI)(tr).toResource
    } yield backend
  }

  private def createBackend(redisUri: String)(implicit tr: Transactor[IO]): IO[SttpBackend[IO, Any]] =
    for {
      random <- Random.scalaUtilRandom[IO]
      redis = new RedisClient(URI.create(redisUri))

      sessionDao = new SessionDaoRedis[IO](redis, config.session.expiration.toSeconds)
      userDao = new UserDaoPostgresql[IO]
      offerDao = new OfferDaoPostgresql[IO]

      authService = new AuthServiceImpl[IO](userDao, sessionDao, config.session.expiration.toSeconds)
      userService = new UserServiceImpl[IO](userDao, random)
      offerService = new OfferServiceImpl[IO](offerDao)

      authHandler = new AuthHandler[IO](authService, userService)
      userHandler = new UserHandler[IO](userService, authService)
      offerHandler = new OfferHandler[IO](offerService, authService)
      endpoints = List(authHandler, userHandler, offerHandler).flatMap(_.endpoints)

      stub = SttpBackendStub(new CatsMonadError[IO]())
      sttpBackend = TapirStubInterpreter(stub).whenServerEndpointsRunLogic(endpoints).backend()
    } yield sttpBackend
}
