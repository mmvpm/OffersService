package com.github.mmvpm.bot.manager.ofs

import cats.Monad
import cats.data.EitherT
import cats.effect.std.Random
import cats.implicits.{toFunctorOps, toTraverseOps}
import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.client.ofs.{OfsClient, error}
import com.github.mmvpm.bot.client.ofs.error.OfsApiClientError
import com.github.mmvpm.bot.client.ofs.response.{CreateOfferResponse, UserIdResponse}
import com.github.mmvpm.bot.manager.ofs.OfsManagerImpl._
import com.github.mmvpm.bot.manager.ofs.error.OfsError
import com.github.mmvpm.bot.manager.ofs.error.OfsError._
import com.github.mmvpm.bot.manager.ofs.response.LoginOrRegisterResponse
import com.github.mmvpm.bot.manager.ofs.response.LoginOrRegisterResponse._
import com.github.mmvpm.bot.state.Storage
import com.github.mmvpm.model.{OfferDescription, Session}
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
      case Some(session) => checkSession(session).map(_ => LoggedIn(getName))
    }

  override def createOffer(description: OfferDescription)(implicit message: Message): EitherT[F, OfsError, Unit] =
    sessionStorage.get match {
      case None          => EitherT.leftT(InvalidSession)
      case Some(session) => createOffer(session, description).void
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
      .leftMap {
        case OfsApiClientError(_, _, _) => InvalidSession: OfsError
        case error                      => OfsSomeError(error.details)
      }

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

  private def createOffer(session: Session, description: OfferDescription): EitherT[F, OfsError, CreateOfferResponse] =
    ofsClient
      .createOffer(session, description)
      .leftMap {
        case OfsApiClientError(_, StatusCode.Unauthorized.code, _) => InvalidSession: OfsError
        case error                                                 => OfsSomeError(error.details)
      }
}

object OfsManagerImpl {
  private val PasswordLength = 10
}
