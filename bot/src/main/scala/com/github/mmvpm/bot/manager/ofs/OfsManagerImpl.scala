package com.github.mmvpm.bot.manager.ofs

import cats.data.EitherT
import cats.effect.std.Random
import cats.implicits.{toFunctorOps, toTraverseOps}
import cats.{Functor, Monad}
import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.client.ofs.OfsClient
import com.github.mmvpm.bot.client.ofs.error.{OfsApiClientError, OfsClientError}
import com.github.mmvpm.bot.client.ofs.response.{OfsOffer, UserIdResponse}
import com.github.mmvpm.bot.manager.ofs.OfsManagerImpl._
import com.github.mmvpm.bot.manager.ofs.error.OfsError
import com.github.mmvpm.bot.manager.ofs.error.OfsError._
import com.github.mmvpm.bot.manager.ofs.response.LoginOrRegisterResponse
import com.github.mmvpm.bot.manager.ofs.response.LoginOrRegisterResponse._
import com.github.mmvpm.bot.model.{OfferPatch, TgPhoto}
import com.github.mmvpm.bot.state.Storage
import com.github.mmvpm.model._
import sttp.model.StatusCode

class OfsManagerImpl[F[_]: Monad](ofsClient: OfsClient[F], sessionStorage: Storage[Option[Session]], random: Random[F])
    extends OfsManager[F] {

  override def login(implicit message: Message): EitherT[F, OfsError, LoggedIn] =
    ofsClient
      .signIn(getLogin, message.text.get)
      .map { response =>
        sessionStorage.set(Some(response.session))
        LoggedIn(getName)
      }
      .leftMap(error => OfsSomeError(error.details))

  override def loginOrRegister(implicit message: Message): EitherT[F, OfsError, LoginOrRegisterResponse] =
    sessionStorage.get match {
      case None          => registerAndSaveSession
      case Some(session) => checkSession(session).as(LoggedIn(getName))
    }

  def search(query: String): EitherT[F, OfsError, List[Offer]] =
    (for {
      offerIds <- ofsClient.search(query).map(_.offerIds)
      offers <- ofsClient.getOffers(offerIds).map(_.offers)
    } yield offers).handleDefaultErrors

  def getOffer(offerId: OfferID): EitherT[F, OfsError, Option[Offer]] =
    ofsClient
      .getOffer(offerId)
      .map(response => Option(response.offer))
      .recover { case OfsApiClientError(_, StatusCode.NotFound.code, _) =>
        None
      }
      .handleDefaultErrors

  def getOffers(offerIds: Seq[OfferID]): EitherT[F, OfsError, List[Offer]] =
    ofsClient
      .getOffers(offerIds)
      .map(_.offers)
      .handleDefaultErrors

  def getMyOffers(implicit message: Message): EitherT[F, OfsError, List[Offer]] =
    sessionStorage.get match {
      case None          => EitherT.leftT(InvalidSession)
      case Some(session) => getMyOffers(session)
    }

  def createOffer(description: OfferDescription)(implicit message: Message): EitherT[F, OfsError, OfsOffer] =
    sessionStorage.get match {
      case None          => EitherT.leftT(InvalidSession)
      case Some(session) => createOffer(session, description)
    }

  def updateOffer(offerId: OfferID, patch: OfferPatch)(implicit message: Message): EitherT[F, OfsError, Unit] =
    sessionStorage.get match {
      case None          => EitherT.leftT(InvalidSession)
      case Some(session) => updateOffer(session, offerId, patch)
    }

  def deleteOffer(offerId: OfferID)(implicit message: Message): EitherT[F, OfsError, Unit] =
    sessionStorage.get match {
      case None          => EitherT.leftT(InvalidSession)
      case Some(session) => deleteOffer(session, offerId)
    }

  def addOfferPhotos(offerId: OfferID, photos: Seq[TgPhoto])(implicit message: Message): EitherT[F, OfsError, Unit] =
    sessionStorage.get match {
      case None          => EitherT.leftT(InvalidSession)
      case Some(session) => addOfferPhotos(session, offerId, photos)
    }

  def deleteAllPhotos(offerId: OfferID)(implicit message: Message): EitherT[F, OfsError, Unit] =
    sessionStorage.get match {
      case None          => EitherT.leftT(InvalidSession)
      case Some(session) => deleteAllPhotos(session, offerId)
    }

  // internal

  private def getLogin(implicit message: Message): String =
    message.chat.username.getOrElse("@noname")

  private def getName(implicit message: Message): String =
    (for {
      firstName <- message.chat.firstName
      lastName <- message.chat.lastName
      name = s"$firstName $lastName"
    } yield name).getOrElse("No Name")

  private def generatePassword: F[String] =
    (0 until PasswordLength).toList
      .traverse(_ => random.nextAlphaNumeric)
      .map(_.mkString)

  private def checkSession(session: Session): EitherT[F, OfsError, UserIdResponse] =
    ofsClient
      .whoami(session)
      .handleDefaultErrors

  private def registerAndSaveSession(implicit message: Message): EitherT[F, OfsError, LoginOrRegisterResponse] =
    (for {
      password <- EitherT.liftF(generatePassword)
      _ <- ofsClient.signUp(getName, getLogin, password)
      session <- ofsClient.signIn(getLogin, password).map(_.session)
      _ = sessionStorage.set(Some(session))
    } yield Registered(password): LoginOrRegisterResponse)
      .leftMap {
        case OfsApiClientError(_, _, _) => InvalidSession: OfsError
        case error                      => OfsSomeError(error.details)
      }

  private def getMyOffers(session: Session): EitherT[F, OfsError, List[Offer]] =
    ofsClient
      .getMyOffers(session)
      .map(_.offers.filter(_.status.isActive))
      .handleDefaultErrors

  private def createOffer(session: Session, description: OfferDescription): EitherT[F, OfsError, OfsOffer] =
    ofsClient
      .createOffer(session, description)
      .map(_.offer)
      .handleDefaultErrors

  private def deleteOffer(session: Session, offerId: OfferID): EitherT[F, OfsError, Unit] =
    ofsClient
      .deleteOffer(session, offerId)
      .void
      .handleDefaultErrors

  private def addOfferPhotos(session: Session, offerId: OfferID, photos: Seq[TgPhoto]): EitherT[F, OfsError, Unit] =
    ofsClient
      .addPhotos(session, offerId, photos)
      .void
      .handleDefaultErrors

  private def deleteAllPhotos(session: Session, offerId: OfferID): EitherT[F, OfsError, Unit] =
    ofsClient
      .deleteAllPhotos(session, offerId)
      .void
      .handleDefaultErrors

  private def updateOffer(session: Session, offerId: OfferID, patch: OfferPatch): EitherT[F, OfsError, Unit] =
    ofsClient
      .updateOffer(session, offerId, patch)
      .void
      .handleDefaultErrors
}

object OfsManagerImpl {

  private val PasswordLength = 10

  implicit class RichClientResponse[F[_]: Functor, R](response: EitherT[F, OfsClientError, R]) {
    def handleDefaultErrors: EitherT[F, OfsError, R] =
      response.leftMap {
        case OfsApiClientError(_, StatusCode.Unauthorized.code, _) => InvalidSession: OfsError
        case error                                                 => OfsSomeError(error.details)
      }
  }
}
