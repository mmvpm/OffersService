package com.github.mmvpm.nemia.api.auth

import cats.effect.{IO, Resource}
import cats.effect.std.Random
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.github.mmvpm.nemia.api.AuthHandler
import com.github.mmvpm.nemia.api.util.{ConfigSupport, PostgresContainerSupport, RedisContainerSupport}
import com.github.mmvpm.nemia.api.util.request.AuthRequestsSupport
import com.github.mmvpm.nemia.dao.session.SessionDaoRedis
import com.github.mmvpm.nemia.dao.user.UserDaoPostgresql
import com.github.mmvpm.nemia.service.auth.AuthServiceImpl
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

class AuthHandlerSpec
  extends FixtureAsyncWordSpec
    with AsyncIOSpec
    with CatsResourceIO[Transactor[IO]]
    with Matchers
    with ConfigSupport
    with RedisContainerSupport
    with PostgresContainerSupport
    with AuthRequestsSupport
    with Logging {

  "AuthHandler" should {
    "create a new user" in { implicit p =>
      for {
        backendStub <- createBackend
        response = signUp("login", "pass")(backendStub)
        _ = log.info(s"### 1 [signUp]: $response")
        _ <- response.asserting { case Right(userResponse) =>
          userResponse.user.description.login shouldBe "login"
        }
      } yield ()
    }

    "sign in correctly" in { implicit p =>
      for {
        backend <- createBackend
        p <- signUp("login", "pass")(backend)
        _ = log.info(s"### 2 [signUp]: $p")
        response = signIn("login", "pass")(backend)
        _ = log.info(s"### 2 [signIn]: $response")
        _ <- response.asserting(_.isRight shouldBe true)
      } yield ()
    }

    "return user id by session" in { implicit p =>
      for {
        backend <- createBackend
        user <- signUp("login", "pass")(backend)
        _ = log.info(s"### 3 [signUp]: $user")
        session <- signIn("login", "pass")(backend)
        _ = log.info(s"### 3 [signIn]: $session")
        response = whoami(session.toOption.get.session)(backend)
        _ = log.info(s"### 3 [whoami]: $response")
        _ <- response.asserting { case Right(userId) =>
          userId.userId shouldBe user.toOption.get.user.id
        }
      } yield ()
    }
  }

  override val resource: Resource[IO, Transactor[IO]] =
    for {
      _ <- makeRedisContainer
      tr <- makePostgresTransactor
    } yield tr

  private def createBackend(implicit tr: Transactor[IO]): IO[SttpBackend[IO, Any]] =
    for {
      random <- Random.scalaUtilRandom[IO]
      redis = new RedisClient(config.redis.host, config.redis.port)

      sessionDao = new SessionDaoRedis[IO](redis, config.session.expiration.toSeconds)
      userDao = new UserDaoPostgresql[IO]()

      authService = new AuthServiceImpl[IO](userDao, sessionDao, config.session.expiration.toSeconds)
      userService = new UserServiceImpl[IO](userDao, random)

      handler = new AuthHandler[IO](authService, userService)

      stub = SttpBackendStub(new CatsMonadError[IO]())
      sttpBackend = TapirStubInterpreter(stub)
        .whenServerEndpointsRunLogic(handler.endpoints)
        .backend()
    } yield sttpBackend
}
