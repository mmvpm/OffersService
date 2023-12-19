package com.github.mmvpm.nemia.api.offer

import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.util.{ConfigSupport, SttpBackendSupport}
import com.github.mmvpm.nemia.api.util.request.{AuthRequestsSupport, OfferRequestsSupport}
import com.github.mmvpm.nemia.api.util.EitherUtils.RichEither
import com.github.mmvpm.util.Logging
import com.github.mmvpm.util.StringUtils.RichString
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.FixtureAsyncWordSpec
import sttp.client3.SttpBackend

class OfferHandlerSpec
  extends FixtureAsyncWordSpec
    with AsyncIOSpec
    with CatsResourceIO[SttpBackend[IO, Any]]
    with Matchers
    with ConfigSupport
    with SttpBackendSupport
    with OfferRequestsSupport
    with AuthRequestsSupport
    with Logging {

  "OfferHandler" should {

    "create a new offer" in { implicit b =>
      for {
        login <- IO.pure("login")
        password <- IO.pure("pass")
        _ <- signUp(login, password)
        session <- signIn(login, password).map(_.sure.session)
        description = OfferDescription("name", 123, "text", List(Photo("https://ya.ru".toURL)))
        _ <- createOffer(session, description).asserting(_.response.offer.description shouldBe description)
      } yield ()
    }
  }

  override val resource: Resource[IO, SttpBackend[IO, Any]] = backend
}
