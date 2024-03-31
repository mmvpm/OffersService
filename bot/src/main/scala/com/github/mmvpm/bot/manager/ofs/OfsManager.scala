package com.github.mmvpm.bot.manager.ofs

import cats.data.EitherT
import com.bot4s.telegram.models.{Chat, Message}
import com.github.mmvpm.bot.manager.ofs.error.OfsError
import com.github.mmvpm.bot.manager.ofs.response.LoginOrRegisterResponse
import com.github.mmvpm.model.OfferDescription

trait OfsManager[F[_]] {
  def login(implicit message: Message): EitherT[F, OfsError, LoginOrRegisterResponse.LoggedIn]
  def loginOrRegister(implicit message: Message): EitherT[F, OfsError, LoginOrRegisterResponse]
  def createOffer(description: OfferDescription)(implicit message: Message): EitherT[F, OfsError, Unit]
}
