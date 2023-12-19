package com.github.mmvpm.nemia.api.user

import cats.effect.{IO, Resource}
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.request._
import com.github.mmvpm.nemia.api.response.ApiUserDescription
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

  it should "get user" in { implicit b =>
    for {
      login <- IO.pure("login")
      password <- IO.pure("pass")
      user <- signUp(login, password).map(_.response.user)
      _ <- getUser(user.id).asserting(_.response.user shouldBe user)
    } yield ()
  }

  it should "return 404 if user is not found" in { implicit b =>
    for {
      nonExistentUserId <- UUIDGen[IO].randomUUID
      _ <- getUser(nonExistentUserId).asserting(_.error.code shouldBe StatusCode.NotFound)
    } yield ()
  }

  it should "update user" in { implicit b =>
    for {
      login <- IO.pure("login1")
      password <- IO.pure("pass1")
      session <- auth(login, password)

      updateRequest = UpdateUserRequest(Some("upd-pass"), Some("upd@email.com"), Some("+79001110022"))
      finalDescription = ApiUserDescription(login, Some("upd@email.com"), Some("+79001110022"))

      _ <- updateUser(session, updateRequest).asserting(_.response.user.description shouldBe finalDescription)
    } yield ()
  }

  it should "delete user" in { implicit b =>
    for {
      login <- IO.pure("delete-login")
      password <- IO.pure("delete-pass")
      user <- signUp(login, password).map(_.response.user)
      session <- signIn(login, password).map(_.response.session)
      _ <- deleteUser(session)
      _ <- getUser(user.id).asserting(_.response.user.status shouldBe UserStatus.Deleted)
    } yield ()
  }

  it should "rate another user" in { implicit b =>
    for {
      anotherLogin <- IO.pure("another-login")
      anotherPassword <- IO.pure("another-pass")
      anotherUser <- signUp(anotherLogin, anotherPassword).map(_.response.user)

      login <- IO.pure("login")
      password <- IO.pure("pass")
      session <- auth(login, password)

      _ <- rateUser(session, anotherUser.id, mark = 9)
      _ <- rateUser(session, anotherUser.id, mark = 8)
      _ <- rateUser(session, anotherUser.id, mark = 8)

      _ <- getUser(anotherUser.id).asserting(_.response.user.rating.marks shouldBe List(8))
    } yield ()
  }

  override val resource: Resource[IO, SttpBackend[IO, Any]] = backend
}
