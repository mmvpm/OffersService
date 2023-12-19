package com.github.mmvpm.nemia.api.user

import cats.effect.{IO, Resource}
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.request._
import com.github.mmvpm.nemia.api.util.SttpBackendSupport
import com.github.mmvpm.nemia.api.util.request._
import com.github.mmvpm.nemia.api.util.EitherUtils.RichEither
import com.github.mmvpm.util.Logging
import com.github.mmvpm.util.StringUtils.RichString
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.SttpBackend
import sttp.model.StatusCode

class UserHandlerSpec
  extends FixtureAsyncFlatSpec
    with AsyncIOSpec
    with CatsResourceIO[SttpBackend[IO, Any]]
    with Matchers
    with SttpBackendSupport
    with UserRequestsSupport
    with AuthRequestsSupport
    with Logging {

  it should "get my profile" in { implicit b =>
    for {
      login <- IO.pure("login")
      password <- IO.pure("pass")
      user <- signUp(login, password).map(_.response.user)
      _ <- getUser(user.id).asserting(_.response.user shouldBe user)
    } yield ()
  }

  override val resource: Resource[IO, SttpBackend[IO, Any]] = backend
}
