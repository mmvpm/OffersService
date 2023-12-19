//noinspection TypeAnnotation
package com.github.mmvpm.nemia.service.offer

import cats.data.EitherT
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.error.OfferNotFoundApiError
import com.github.mmvpm.nemia.api.request.CreateOfferRequest
import com.github.mmvpm.nemia.api.response.OfferResponse
import com.github.mmvpm.nemia.dao.error.OfferNotFoundDaoError
import com.github.mmvpm.nemia.dao.offer.OfferDao
import com.github.mmvpm.nemia.service.offer.OfferServiceImpl
import com.github.mmvpm.util.StringUtils.RichString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.Instant.now
import java.util.UUID

class OfferServiceImplSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Fixture {

  "OfferService" - {
    "return existing offer" in {
      val offer = createOffer()

      when(offerDao.getOffers(any)).thenReturn(EitherT.pure(List(offer)))

      offerService.getOffer(offer.id).value.asserting(_ shouldBe Right(OfferResponse(offer)))
    }
    "return not found on non-existent offer" in {
      val offerId = UUID.randomUUID()

      when(offerDao.getOffers(any)).thenReturn(EitherT.fromEither(Left(OfferNotFoundDaoError(offerId))))

      offerService.getOffer(offerId).value.asserting(_ shouldBe Left(OfferNotFoundApiError(offerId)))
    }
    "create a new offer" in {
      val offer = createOffer()
      val request = CreateOfferRequest(offer.description)

      when(offerDao.createOffer(any)).thenReturn(EitherT.pure(()))

      offerService.createOffer(offer.userId, request).value.asserting { result =>
        result.map(_.offer.description) shouldBe Right(offer.description)
      }
    }
  }

  private def createOffer(): Offer =
    Offer(
      id = UUID.randomUUID(),
      userId = UUID.randomUUID(),
      description = OfferDescription(
        name = "name",
        price = 15,
        text = "text",
        photos = List(Photo("https://ya.ru".toURL))
      ),
      status = OfferStatus.Active,
      createdAt = now,
      updatedAt = now
    )
}


trait Fixture {
  val offerDao = mock[OfferDao[IO]]
  val offerService = new OfferServiceImpl[IO](offerDao)
}
