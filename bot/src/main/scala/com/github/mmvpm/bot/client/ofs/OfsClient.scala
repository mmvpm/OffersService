package com.github.mmvpm.bot.client.ofs

import cats.data.EitherT
import com.github.mmvpm.model.{OfferDescription, Session}
import com.github.mmvpm.bot.client.ofs.error.OfsClientError
import com.github.mmvpm.bot.client.ofs.response.{CreateOfferResponse, SignInResponse, SignUpResponse, UserIdResponse}

trait OfsClient[F[_]] {

  def signUp(name: String, login: String, password: String): EitherT[F, OfsClientError, SignUpResponse]
  def signIn(login: String, password: String): EitherT[F, OfsClientError, SignInResponse]
  def whoami(session: Session): EitherT[F, OfsClientError, UserIdResponse]

  def createOffer(session: Session, description: OfferDescription): EitherT[F, OfsClientError, CreateOfferResponse]
}
