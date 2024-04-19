package com.github.mmvpm.parsing.client.ofs

import cats.data.EitherT
import com.github.mmvpm.model.{OfferDescription, Session}
import com.github.mmvpm.parsing.client.ofs.error.OfsError
import com.github.mmvpm.parsing.client.ofs.response.{CreateOfferResponse, SignInResponse, SignUpResponse}

trait OfsClient[F[_]] {
  def signUp(name: String, login: String, password: String): EitherT[F, OfsError, SignUpResponse]
  def signIn(login: String, password: String): EitherT[F, OfsError, SignInResponse]
  def createOffer(session: Session, description: OfferDescription): EitherT[F, OfsError, CreateOfferResponse]
}
