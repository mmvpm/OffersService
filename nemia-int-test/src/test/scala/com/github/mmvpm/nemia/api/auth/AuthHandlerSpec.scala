package com.github.mmvpm.nemia.api.auth

import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.github.mmvpm.nemia.api.util.SttpBackendSupport
import com.github.mmvpm.nemia.api.util.request.AuthRequestsSupport
import com.github.mmvpm.nemia.api.util.EitherUtils.RichEither
import com.github.mmvpm.util.Logging
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.SttpBackend

class AuthHandlerSpec
  extends FixtureAsyncFlatSpec
  with AsyncIOSpec
  with CatsResourceIO[SttpBackend[IO, Any]]
  with Matchers
  with SttpBackendSupport
  with AuthRequestsSupport
  with Logging {

  it should "create a new user" in { implicit b =>
    for {
      login <- IO.pure("login")
      password <- IO.pure("pass")
      _ <- signUp(login, password).asserting(_.response.user.description.login shouldBe login)
    } yield ()
  }

  it should "sign in correctly" in { implicit b =>
    for {
      login <- IO.pure("login2")
      password <- IO.pure("pass2")
      _ <- signUp(login, password)
      _ <- signIn(login, password).asserting(_.isRight shouldBe true)
    } yield ()
  }

  it should "return user id by session" in { implicit b =>
    for {
      login <- IO.pure("login3")
      password <- IO.pure("pass3")
      user <- signUp(login, password).map(_.response.user)
      session <- signIn(login, password).map(_.response.session)
      _ <- whoami(session).asserting(_.response.userId shouldBe user.id)
    } yield ()
  }

  override val resource: Resource[IO, SttpBackend[IO, Any]] = backend
}
