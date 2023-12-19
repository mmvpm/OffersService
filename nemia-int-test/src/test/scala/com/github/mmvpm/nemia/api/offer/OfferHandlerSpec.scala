package com.github.mmvpm.nemia.api.offer

import cats.effect.{IO, Resource}
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.{AsyncIOSpec, CatsResourceIO}
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.request.UpdateOfferRequest
import com.github.mmvpm.nemia.api.util.{ConfigSupport, SttpBackendSupport}
import com.github.mmvpm.nemia.api.util.request.{AuthRequestsSupport, OfferRequestsSupport}
import com.github.mmvpm.nemia.api.util.EitherUtils.RichEither
import com.github.mmvpm.util.Logging
import com.github.mmvpm.util.StringUtils.RichString
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.SttpBackend
import sttp.model.StatusCode

class OfferHandlerSpec
  extends FixtureAsyncFlatSpec
  with AsyncIOSpec
  with CatsResourceIO[SttpBackend[IO, Any]]
  with Matchers
  with ConfigSupport
  with SttpBackendSupport
  with OfferRequestsSupport
  with AuthRequestsSupport
  with Logging {

  it should "create a new offer" in { implicit b =>
    for {
      login <- IO.pure("login")
      password <- IO.pure("pass")
      session <- auth(login, password)
      description = OfferDescription("name", 123, "text", List(Photo("https://ya.ru".toURL)))
      _ <- createOffer(session, description).asserting(_.response.offer.description shouldBe description)
    } yield ()
  }

  it should "return created offer" in { implicit b =>
    for {
      login <- IO.pure("login")
      password <- IO.pure("pass")
      session <- auth(login, password)
      description = OfferDescription("name", 123, "text", List(Photo("https://ya.ru".toURL)))
      offer <- createOffer(session, description).map(_.response.offer)
      _ <- getOffer(offer.id).asserting(_.response.offer shouldBe offer)
    } yield ()
  }

  it should "return not found when offer id is unknown" in { implicit b =>
    for {
      nonExistentOfferId <- UUIDGen[IO].randomUUID
      _ <- getOffer(nonExistentOfferId).asserting(_.error.code shouldBe StatusCode.NotFound)
    } yield ()
  }

  it should "update offer" in { implicit b =>
    for {
      login <- IO.pure("login")
      password <- IO.pure("pass")
      session <- auth(login, password)

      description = OfferDescription("name", 123, "text", List(Photo("https://ya.ru".toURL)))
      updateRequest = UpdateOfferRequest(Some("upd-name"), Some(456), Some("upd-text"), Some(List.empty))
      finalDescription = OfferDescription("upd-name", 456, "upd-text", List.empty)

      offer <- createOffer(session, description).map(_.response.offer)
      _ <- updateOffer(session, offer.id, updateRequest)
        .asserting(_.response.offer.description shouldBe finalDescription)
    } yield ()
  }

  it should "delete offer" in { implicit b =>
    for {
      login <- IO.pure("login")
      password <- IO.pure("pass")
      session <- auth(login, password)

      description = OfferDescription("name", 123, "text", List(Photo("https://ya.ru".toURL)))
      offer <- createOffer(session, description).map(_.response.offer)
      _ <- deleteOffer(session, offer.id)

      _ <- getOffer(offer.id).asserting(_.error.code shouldBe StatusCode.NotFound)
    } yield ()
  }

  override val resource: Resource[IO, SttpBackend[IO, Any]] = backend
}
