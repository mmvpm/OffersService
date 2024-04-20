package com.github.mmvpm.service.service.offer

import cats.data.EitherT
import com.github.mmvpm.model.{OfferID, UserID}
import com.github.mmvpm.service.api.error.ApiError
import com.github.mmvpm.service.api.request.{AddOfferPhotosRequest, CreateOfferRequest, GetOffersRequest, UpdateOfferRequest}
import com.github.mmvpm.service.api.response.{OfferIdsResponse, OfferResponse, OffersResponse, OkResponse}

trait OfferService[F[_]] {
  def getOffer(offerId: OfferID): EitherT[F, ApiError, OfferResponse]
  def getOffers(request: GetOffersRequest): EitherT[F, ApiError, OffersResponse]
  def getOffers(userId: UserID): EitherT[F, ApiError, OffersResponse]
  def createOffer(userId: UserID, request: CreateOfferRequest): EitherT[F, ApiError, OfferResponse]
  def updateOffer(userId: UserID, offerId: OfferID, request: UpdateOfferRequest): EitherT[F, ApiError, OfferResponse]
  def deleteOffer(userId: UserID, offerId: OfferID): EitherT[F, ApiError, OkResponse]
  def addPhotos(userId: UserID, offerId: OfferID, request: AddOfferPhotosRequest): EitherT[F, ApiError, OfferResponse]
  def deleteAllPhotos(userId: UserID, offerId: OfferID): EitherT[F, ApiError, OkResponse]
  def search(query: String, limit: Int): EitherT[F, ApiError, OfferIdsResponse]
}
