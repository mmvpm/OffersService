package com.github.mmvpm.parseidon.client.nemia

import cats.data.EitherT
import com.github.mmvpm.model.{OfferDescription, Session, UserDescriptionRaw}
import com.github.mmvpm.parseidon.client.nemia.error.NemiaError
import com.github.mmvpm.parseidon.client.nemia.response.{CreateOfferResponse, SignInResponse, SignUpResponse}

trait NemiaClient[F[_]] {
  def signUp(user: UserDescriptionRaw): EitherT[F, NemiaError, SignUpResponse]
  def signIn(login: String, password: String): EitherT[F, NemiaError, SignInResponse]
  def createOffer(session: Session, description: OfferDescription): EitherT[F, NemiaError, CreateOfferResponse]
}
