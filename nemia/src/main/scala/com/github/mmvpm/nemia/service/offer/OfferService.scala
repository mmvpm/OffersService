package com.github.mmvpm.nemia.service.offer

import cats.data.EitherT
import com.github.mmvpm.nemia.api.request.{CreateOfferRequest, UpdateOfferRequest}
import com.github.mmvpm.nemia.api.response.{OfferResponse, OffersResponse, OkResponse}
import com.github.mmvpm.model.{OfferID, UserID}
import com.github.mmvpm.nemia.api.error.ApiError

trait OfferService[F[_]] {
  def getOffer(offerId: OfferID): EitherT[F, ApiError, OfferResponse]
  def getOffers(userId: UserID): EitherT[F, ApiError, OffersResponse]
  def createOffer(userId: UserID, request: CreateOfferRequest): EitherT[F, ApiError, OfferResponse]
  def updateOffer(userId: UserID, offerId: OfferID, request: UpdateOfferRequest): EitherT[F, ApiError, OfferResponse]
  def deleteOffer(userId: UserID, offerId: OfferID): EitherT[F, ApiError, OkResponse]
}
