package com.github.mmvpm.service.api

import cats.Applicative
import com.github.mmvpm.model.{OfferID, UserID}
import com.github.mmvpm.service.api.request.{
  AddOfferPhotosRequest,
  CreateOfferRequest,
  GetOffersRequest,
  UpdateOfferRequest
}
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
      .summary("Get the offer by its ID")
      .in("api" / "v1" / "offer" / path[OfferID]("offer-id"))
      .out(jsonBody[OfferResponse])
      .serverLogic(offerService.getOffer(_).value)

  private val getOffersByIds: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.post
      .summary("Get all offers by ids")
      .in("api" / "v1" / "offer" / "list")
      .in(jsonBody[GetOffersRequest])
      .out(jsonBody[OffersResponse])
      .serverLogic(offerService.getOffers(_).value)

  private val getOffers: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.get
      .summary("Get all offers of the specified user")
      .in("api" / "v1" / "offer" / "list" / "user" / path[UserID]("user-id"))
      .out(jsonBody[OffersResponse])
      .serverLogic(offerService.getOffers(_).value)

  private val getMyOffers: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.get
      .summary("Get all my offers")
      .in("api" / "v1" / "offer" / "list" / "my")
      .out(jsonBody[OffersResponse])
      .serverLogic(userId => _ => offerService.getOffers(userId).value)

  private val createOffer: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.post
      .summary("Create an offer")
      .in("api" / "v1" / "offer")
      .in(jsonBody[CreateOfferRequest])
      .out(jsonBody[OfferResponse])
      .serverLogic(userId => request => offerService.createOffer(userId, request).value)

  private val updateOffer: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.put
      .summary("Update the offer")
      .in("api" / "v1" / "offer" / path[OfferID]("offer-id"))
      .in(jsonBody[UpdateOfferRequest])
      .out(jsonBody[OfferResponse])
      .serverLogic(userId => { case (offerId, request) =>
        offerService.updateOffer(userId, offerId, request).value
      })

  private val deleteOffer: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.delete
      .summary("Delete the offer")
      .in("api" / "v1" / "offer" / path[OfferID]("offer-id"))
      .out(jsonBody[OkResponse])
      .serverLogic(userId => offerId => offerService.deleteOffer(userId, offerId).value)

  private val addOfferPhotos: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.put
      .summary("Add photos to the offer")
      .in("api" / "v1" / "offer" / path[OfferID]("offer-id") / "photo")
      .in(jsonBody[AddOfferPhotosRequest])
      .out(jsonBody[OfferResponse])
      .serverLogic(userId => { case (offerId, request) =>
        offerService.addPhotos(userId, offerId, request).value
      })

  private val deleteAllOfferPhotos: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.delete
      .summary("Delete all photos from the offer")
      .in("api" / "v1" / "offer" / path[OfferID]("offer-id") / "photo" / "all")
      .out(jsonBody[OkResponse])
      .serverLogic(userId => offerId => offerService.deleteAllPhotos(userId, offerId).value)

  override def endpoints: List[ServerEndpoint[Any, F]] =
    List(
      getOffer,
      getOffersByIds,
      getOffers,
      getMyOffers,
      createOffer,
      updateOffer,
      deleteOffer,
      addOfferPhotos,
      deleteAllOfferPhotos
    ).map(
      _.withTag("offer")
    )
}
