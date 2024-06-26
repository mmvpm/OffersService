package com.github.mmvpm.bot.state

import cats.data.EitherT
import cats.effect.kernel.MonadCancelThrow
import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps}
import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.manager.ofs.OfsManager
import com.github.mmvpm.bot.manager.ofs.error.OfsError
import com.github.mmvpm.bot.manager.ofs.error.OfsError.InvalidSession
import com.github.mmvpm.bot.model.{Draft, OfferPatch, TgPhoto}
import com.github.mmvpm.bot.state.State.{Listing, _}
import com.github.mmvpm.bot.util.StateUtils.StateSyntax
import com.github.mmvpm.bot.util.StringUtils.RichString

class StateManagerImpl[F[_]: MonadCancelThrow](ofsManager: OfsManager[F]) extends StateManager[F] {

  override def getNextState(tag: String, current: State)(implicit message: Message): F[State] =
    tag match {
      case SearchTag                 => toSearch(current)
      case ListingTag                => toListing(current)
      case Listing.chooseOne(idx)    => toOneOfferIdx(current, idx.toInt)
      case OneOfferTag               => toOneOffer(current)
      case CreateOfferNameTag        => toCreateOfferName(current)
      case CreateOfferPriceTag       => toCreateOfferPrice(current)
      case CreateOfferDescriptionTag => toCreateOfferDescription(current)
      case CreateOfferPhotoTag       => toCreateOfferPhoto(current)
      case CreatedOfferTag           => toCreatedOffer(current)
      case MyOffersTag               => toMyOffers(current)
      case MyOffers.chooseOne(idx)   => toMyOfferIdx(current, idx.toInt)
      case MyOfferTag                => toMyOffer(current)
      case EditOfferTag              => toEditOffer(current)
      case EditOfferNameTag          => toEditOfferName(current)
      case EditOfferPriceTag         => toEditOfferPrice(current)
      case EditOfferDescriptionTag   => toEditOfferDescription(current)
      case AddOfferPhotoTag          => toAddOfferPhoto(current)
      case DeleteOfferPhotosTag      => toDeleteOfferPhotos(current)
      case UpdatedOfferTag           => toUpdatedOffer(current)
      case DeletedOfferTag           => toDeleteOffer(current)
      case LoggedInTag               => toLoggedIn(current)
      case BackTag                   => toBack(current)
      case StartedTag                => toStarted(current)
    }

  // transitions

  private def toSearch(current: State)(implicit message: Message): F[State] =
    Search(current).pure

  private def toListing(current: State)(implicit message: Message): F[State] =
    current match {
      case Listing(_, offers, from) =>
        Listing(current, offers, from + Listing.StepSize).pure
      case _ =>
        message.text match {
          case Some(query) =>
            ofsManager
              .search(query)
              .handleDefaultErrors(current, ifSuccess = Listing.start(current, _).pure)
          case None =>
            Error(current, "Пожалуйста, введите поисковый запрос").pure
        }
    }

  private def toOneOffer(current: State)(implicit message: Message): F[State] = {
    val newState = for {
      offerId <- message.text
      offers <- current match {
        case Listing(_, offers, _) => Some(offers)
        case _                     => None
      }
      offer <- offers.find(_.offer.id.toString == offerId)
    } yield OneOffer(current, offer)

    newState.getOrElse(Error(current, "К сожалению, такого id не существует! Попробуйте ещё раз")).pure
  }

  private def toOneOfferIdx(current: State, idx: Int)(implicit message: Message): F[State] =
    findPreviousListing(current) match {
      case Some(listing) => OneOffer(current, listing.get(idx)).pure
      case None          => Error(current, "Произошла ошибка! Попробуйте ещё раз или начните сначала").pure
    }

  private def toCreateOfferName(current: State)(implicit message: Message): F[State] =
    CreateOfferName(current).pure

  private def toCreateOfferPrice(current: State)(implicit message: Message): F[State] = {
    val newState = for {
      name <- message.text
      if name.containsAtLeastOneLetterOrDigit
    } yield CreateOfferPrice(current, Draft(name = Some(name)))

    newState
      .getOrElse(
        Error(current, "Пожалуйста, введите название объявления (должно содержать хотя бы одну букву)")
      )
      .pure
  }

  private def toCreateOfferDescription(current: State)(implicit message: Message): F[State] = {
    val newState = for {
      priceRaw <- message.text
      price <- priceRaw.toIntOption
      if price >= 0
      draft <- current match {
        case CreateOfferPrice(_, draft) => Some(draft)
        case _                          => None
      }
      updatedDraft = draft.copy(price = Some(price))
    } yield CreateOfferDescription(current, updatedDraft)

    newState.getOrElse(Error(current, "Пожалуйста, введите цену (целое число рублей от 0 до 2 млрд)")).pure
  }

  private def toCreateOfferPhoto(current: State)(implicit message: Message): F[State] =
    current match {
      case CreateOfferDescription(_, draft) => // description has been uploaded
        val newState = for {
          description <- message.text
          if description.containsAtLeastOneLetterOrDigit
          updatedDraft = draft.copy(description = Some(description))
        } yield CreateOfferPhoto(current, updatedDraft)

        newState
          .getOrElse(
            Error(current, "Пожалуйста, введите описание к объявлению (должно содержать хотя бы одну букву)")
          )
          .pure

      case CreateOfferPhoto(_, draft) => // another photo has been uploaded
        val newState = for {
          photoWithSizes <- message.photo
          photo <- photoWithSizes.lastOption
          tgPhoto = TgPhoto(None, Some(photo.fileId))
          updatedDraft = draft.copy(photos = draft.photos ++ Seq(tgPhoto))
        } yield CreateOfferPhoto(current, updatedDraft)

        newState.getOrElse(Error(current, "Пожалуйста, загрузите фото")).pure

      case _ =>
        Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toCreatedOffer(current: State)(implicit message: Message): F[State] =
    current match {
      case CreateOfferPhoto(_, draft) if draft.toOfferDescription.nonEmpty =>
        (for {
          offer <- ofsManager.createOffer(draft.toOfferDescription.get)
          _ <- ofsManager.addOfferPhotos(offer.id, draft.photos)
        } yield ()).handleDefaultErrors(current, ifSuccess = _ => CreatedOffer(current, draft).pure)

      case _ =>
        Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toMyOffers(current: State)(implicit message: Message): F[State] =
    current match {
      case MyOffers(_, offers, from) =>
        MyOffers(current, offers, from + MyOffers.StepSize).pure
      case _ =>
        ofsManager.getMyOffers
          .handleDefaultErrors(current, ifSuccess = MyOffers.start(current, _).pure)
    }

  private def toMyOffer(current: State)(implicit message: Message): F[State] = {
    val optOffer = for {
      offerId <- message.text
      if offerId.isUUID
      offers <- current match {
        case MyOffers(_, offers, _) => Some(offers)
        case _                      => None
      }
      offer <- offers.find(_.id == offerId.toUUID)
    } yield offer

    optOffer match {
      case Some(offerId) => MyOffer(current, offerId).pure
      case None          => Error(current, "К сожалению, такого id не существует! Попробуйте ещё раз").pure
    }
  }

  private def toMyOfferIdx(current: State, idx: Int)(implicit message: Message): F[State] =
    findPreviousMyOffers(current) match {
      case Some(myOffers) => MyOffer(current, myOffers.get(idx)).pure
      case None           => Error(current, "Произошла ошибка! Попробуйте ещё раз или начните сначала").pure
    }

  private def toEditOffer(current: State)(implicit message: Message): F[State] =
    current match {
      case state: WithOfferID => EditOffer(current, state.offerId).pure
      case _                  => Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toEditOfferName(current: State)(implicit message: Message): F[State] =
    current match {
      case state: WithOfferID => EditOfferName(current, state.offerId).pure
      case _                  => Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toEditOfferPrice(current: State)(implicit message: Message): F[State] =
    current match {
      case state: WithOfferID => EditOfferPrice(current, state.offerId).pure
      case _                  => Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toEditOfferDescription(current: State)(implicit message: Message): F[State] =
    current match {
      case state: WithOfferID => EditOfferDescription(current, state.offerId).pure
      case _                  => Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toAddOfferPhoto(current: State)(implicit message: Message): F[State] =
    current match {
      case state: WithOfferID => AddOfferPhoto(current, state.offerId).pure
      case _                  => Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toDeleteOfferPhotos(current: State)(implicit message: Message): F[State] =
    current match {
      case state: WithOfferID =>
        ofsManager
          .deleteAllPhotos(state.offerId)
          .handleDefaultErrors(
            current,
            ifSuccess = _ => UpdatedOffer(current, "Все фотографии были удалены из объявления").pure
          )
      case _ =>
        Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toUpdatedOffer(current: State)(implicit message: Message): F[State] =
    current match {
      case EditOfferName(_, offerId) =>
        message.text match {
          case Some(newName) if newName.containsAtLeastOneLetterOrDigit =>
            ofsManager
              .updateOffer(offerId, OfferPatch(name = Some(newName)))
              .handleDefaultErrors(
                current,
                ifSuccess = _ => UpdatedOffer(current, s"Название было изменено на \"$newName\"").pure
              )
          case _ =>
            Error(current, "Пожалуйста, введите новое название объявления (должно содержать хотя бы одну букву)").pure
        }

      case EditOfferPrice(_, offerId) =>
        message.text.flatMap(_.toIntOption).filter(_ >= 0) match {
          case Some(newPrice) =>
            ofsManager
              .updateOffer(offerId, OfferPatch(price = Some(newPrice)))
              .handleDefaultErrors(
                current,
                ifSuccess = _ => UpdatedOffer(current, s"Цена была изменена на $newPrice").pure
              )
          case _ =>
            Error(current, "Пожалуйста, введите новую цену (целое число рублей от 0 до 2 млрд)").pure
        }

      case EditOfferDescription(_, offerId) =>
        message.text match {
          case Some(newDescription) if newDescription.containsAtLeastOneLetterOrDigit =>
            ofsManager
              .updateOffer(offerId, OfferPatch(description = Some(newDescription)))
              .handleDefaultErrors(
                current,
                ifSuccess = _ => UpdatedOffer(current, s"Описание было изменено").pure
              )
          case _ =>
            Error(current, "Пожалуйста, введите новое описание объявления (должно содержать хотя бы одну букву)").pure
        }

      case AddOfferPhoto(_, offerId) =>
        message.photo.flatMap(_.lastOption) match {
          case Some(newPhoto) =>
            val tgPhoto = TgPhoto(None, Some(newPhoto.fileId))
            ofsManager
              .addOfferPhotos(offerId, Seq(tgPhoto))
              .handleDefaultErrors(
                current,
                ifSuccess = _ => UpdatedOffer(current, s"Фотография была добавлена к объявлению").pure
              )
          case None =>
            Error(current, "Пожалуйста, загрузите фото").pure
        }

      case _ =>
        Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toDeleteOffer(current: State)(implicit message: Message): F[State] =
    current match {
      case state: WithOfferID =>
        ofsManager
          .deleteOffer(state.offerId)
          .handleDefaultErrors(current, ifSuccess = _ => DeletedOffer(current).pure)
      case _ =>
        Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toLoggedIn(current: State)(implicit message: Message): F[State] =
    message.text match {
      case Some(_) =>
        ofsManager.login.value.flatMap {
          case Left(error)     => Error(current, s"Ошибка: ${error.details}").pure
          case Right(loggedIn) => LoggedIn(loggedIn.name).pure
        }
      case None =>
        Error(current, s"Пожалуйста, введите пароль").pure
    }

  private def toBack(current: State)(implicit message: Message): F[State] =
    current match {
      case DeletedOffer(previous) =>
        previous.optPrevious.getOrElse(Started).pure
      case UpdatedOffer(previous, _) =>
        val nearestEditOffer = previous match {
          case EditOffer(_, _) => previous
          case _               => previous.optPrevious.getOrElse(Started)
        }
        safeRefreshOffers(nearestEditOffer)
      case _ =>
        current.optPrevious.getOrElse(Started).pure
    }

  private def toStarted(current: State)(implicit message: Message): F[State] =
    Started.pure

  // internal

  implicit class RichOfsManagerResponse[Result](result: EitherT[F, OfsError, Result]) {
    def handleDefaultErrors(current: State, ifSuccess: Result => F[State]): F[State] =
      result.value.flatMap {
        case Right(offers)        => ifSuccess(offers)
        case Left(InvalidSession) => EnterPassword.pure
        case Left(error)          => Error(current, s"Произошла ошибка: ${error.details}. Попробуйте ещё раз").pure
      }
  }

  private def findPreviousMyOffers(state: State): Option[MyOffers] =
    state match {
      case target: MyOffers => Some(target)
      case Started          => None
      case _                => state.optPrevious.flatMap(findPreviousMyOffers)
    }

  private def findPreviousListing(state: State): Option[Listing] =
    state match {
      case target: Listing => Some(target)
      case Started         => None
      case _               => state.optPrevious.flatMap(findPreviousListing)
    }

  private def safeRefreshOffers(current: State): F[State] =
    refreshOffers(current).recover(_ => current)

  private def refreshOffers: State => F[State] = {
    case Started =>
      Started.pure

    case MyOffer(previous, previousOffer) =>
      for {
        result <- ofsManager.getOffer(previousOffer.id).value
        freshOffer = result match {
          case Right(Some(offer)) => offer
          case _                  => previousOffer
        }
        freshPrevious <- refreshOffers(previous)
      } yield MyOffer(freshPrevious, freshOffer)

    case MyOffers(previous, previousOffers, from) =>
      for {
        result <- ofsManager.getOffers(previousOffers.map(_.id)).value
        freshOffers = result match {
          case Right(offers) => offers
          case _             => previousOffers
        }
        freshPrevious <- refreshOffers(previous)
      } yield MyOffers(freshPrevious, freshOffers, from)

    case current if current.optPrevious.nonEmpty =>
      refreshOffers(current.optPrevious.get)

    case current =>
      current.pure
  }
}
