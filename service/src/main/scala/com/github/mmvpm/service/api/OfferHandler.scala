package com.github.mmvpm.service.api

import cats.Applicative
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.{OfferID, UserID}
import com.github.mmvpm.service.api.request._
import com.github.mmvpm.service.api.response.{OfferIdsResponse, OfferResponse, OffersResponse, OkResponse}
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

  // search

  private val search: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.get
      .summary("Search by offers")
      .in("api" / "v1" / "offer" / "search")
      .in(query[String]("query"))
      .in(query[Int]("limit").default(30))
      .out(jsonBody[OfferIdsResponse])
      .serverLogic { case (query, limit) =>
        offerService.search(query, limit).value
      }

  // offer

  private val getOffer: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.get
      .summary("Get the offer by its ID")
      .in("api" / "v1" / "offer" / path[OfferID]("offer-id"))
      .out(jsonBody[OfferResponse])
      .serverLogic(offerService.getOffer(_).value)

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

  // offers

  private val getOffersByIds: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.post
      .summary("Get all offers by ids")
      .in("api" / "v1" / "offer" / "list")
      .in(jsonBody[GetOffersRequest])
      .out(jsonBody[OffersResponse])
      .serverLogic(offerService.getOffers(_).value)

  private val getOffersByStatus: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.get
      .summary("Get all offers by specified status")
      .in("api" / "v1" / "offer" / "list" / "status")
      .in(query[OfferStatus]("status"))
      .in(query[Int]("limit"))
      .out(jsonBody[OffersResponse])
      .serverLogic { case (status, limit) =>
        offerService.getOffers(status, limit).value
      }

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

  private val updateOfferStatusBatch: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.put
      .summary("Update the offer")
      .in("api" / "v1" / "offer" / "list" / "status")
      .in(jsonBody[UpdateOfferStatusBatchRequest])
      .out(jsonBody[OkResponse])
      .serverLogic(offerService.updateOfferStatus(_).value)

  // endpoints

  private val searchEndpoints: List[ServerEndpoint[Any, F]] =
    List(search).map(_.withTag("search"))

  private val offerEndpoints: List[ServerEndpoint[Any, F]] =
    List(
      createOffer,
      getOffer,
      updateOffer,
      deleteOffer,
      addOfferPhotos,
      deleteAllOfferPhotos
    ).map(
      _.withTag("offer")
    )

  private val offersEndpoints: List[ServerEndpoint[Any, F]] =
    List(
      getOffersByIds,
      getOffersByStatus,
      getOffers,
      getMyOffers,
      updateOfferStatusBatch
    ).map(
      _.withTag("offers")
    )

  override def endpoints: List[ServerEndpoint[Any, F]] =
    searchEndpoints ++ offerEndpoints ++ offersEndpoints
}
