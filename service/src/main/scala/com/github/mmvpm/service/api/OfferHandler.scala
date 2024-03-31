package com.github.mmvpm.service.api

import cats.Applicative
import com.github.mmvpm.model.{OfferID, UserID}
import com.github.mmvpm.service.api.request.{CreateOfferRequest, UpdateOfferRequest}
import com.github.mmvpm.service.api.response.{OfferResponse, OffersResponse, OkResponse}
import com.github.mmvpm.service.api.support.{ApiErrorSupport, AuthSessionSupport}
import com.github.mmvpm.service.api.util.CirceInstances._
import com.github.mmvpm.service.api.util.SchemaInstances._
import com.github.mmvpm.service.service.auth.AuthService
import com.github.mmvpm.service.service.offer.OfferService
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint

class OfferHandler[F[_]: Applicative](offerService: OfferService[F], override val authService: AuthService[F])
    extends Handler[F]
    with AuthSessionSupport[F]
    with ApiErrorSupport {

  private val getOffer: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.get
      .summary("Получить объявление по его id")
      .in("api" / "v1" / "offer" / path[OfferID]("offer-id"))
      .out(jsonBody[OfferResponse])
      .serverLogic(offerService.getOffer(_).value)

  private val getOffers: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.get
      .summary("Получить все объявления данного пользователя")
      .in("api" / "v1" / "offer" / "user" / path[UserID]("user-id"))
      .out(jsonBody[OffersResponse])
      .serverLogic(offerService.getOffers(_).value)

  private val createOffer: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.post
      .summary("Создать объявление")
      .in("api" / "v1" / "offer")
      .in(jsonBody[CreateOfferRequest])
      .out(jsonBody[OfferResponse])
      .serverLogic(userId => request => offerService.createOffer(userId, request).value)

  private val updateOffer: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.put
      .summary("Изменить объявление")
      .in("api" / "v1" / "offer" / path[OfferID]("offer-id"))
      .in(jsonBody[UpdateOfferRequest])
      .out(jsonBody[OfferResponse])
      .serverLogic(userId => { case (offerId, request) =>
        offerService.updateOffer(userId, offerId, request).value
      })

  private val deleteOffer: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.delete
      .summary("Удалить объявление")
      .in("api" / "v1" / "offer" / path[OfferID]("offer-id"))
      .out(jsonBody[OkResponse])
      .serverLogic(userId => offerId => offerService.deleteOffer(userId, offerId).value)

  override def endpoints: List[ServerEndpoint[Any, F]] =
    List(getOffer, getOffers, createOffer, updateOffer, deleteOffer).map(_.withTag("offer"))
}
