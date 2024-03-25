package com.github.mmvpm.bot.state

import cats.Monad
import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.model.Draft
import com.github.mmvpm.bot.state.State.{Listing, _}
import com.github.mmvpm.bot.util.StateUtils.StateSyntax
import com.github.mmvpm.model.Stub

import java.util.UUID
import scala.util.Random

class StateManagerImpl[F[_]: Monad] extends StateManager[F] {

  override def getNextState(tag: String, current: State)(implicit message: Message): F[State] =
    tag match {
      case SearchTag                 => toSearch(current)
      case ListingTag                => toListing(current)
      case OneOfferTag               => toOneOffer(current)
      case CreateOfferNameTag        => toCreateOfferName(current)
      case CreateOfferPriceTag       => toCreateOfferPrice(current)
      case CreateOfferDescriptionTag => toCreateOfferDescription(current)
      case CreateOfferPhotoTag       => toCreateOfferPhoto(current)
      case CreatedOfferTag           => toCreatedOffer(current)
      case MyOffersTag               => toMyOffers(current)
      case MyOfferTag                => toMyOffer(current)
      case EditOfferTag              => toEditOffer(current)
      case EditOfferNameTag          => toEditOfferName(current)
      case EditOfferPriceTag         => toEditOfferPrice(current)
      case EditOfferDescriptionTag   => toEditOfferDescription(current)
      case AddOfferPhotoTag          => toAddOfferPhoto(current)
      case DeleteOfferPhotosTag      => toDeleteOfferPhotos(current)
      case UpdatedOfferTag           => toUpdatedOffer(current)
      case DeletedOfferTag           => toDeleteOffer(current)
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
        Listing.start(current, getOffers(20)).pure
    }

  private def toOneOffer(current: State)(implicit message: Message): F[State] = {
    val newState = for {
      offerId <- message.text
      offers <- current match {
        case Listing(_, offers, _) => Some(offers)
        case _                     => None
      }
      offer <- offers.find(_.id.toString == offerId)
    } yield OneOffer(current, offer)

    newState.getOrElse(Error(current, "К сожалению, такого id не существует! Попробуйте ещё раз")).pure
  }

  private def toCreateOfferName(current: State)(implicit message: Message): F[State] =
    CreateOfferName(current).pure

  private def toCreateOfferPrice(current: State)(implicit message: Message): F[State] =
    message.text match {
      case Some(name) => CreateOfferPrice(current, Draft(name = Some(name))).pure
      case _          => Error(current, "Пожалуйста, введите название объявления").pure
    }

  private def toCreateOfferDescription(current: State)(implicit message: Message): F[State] = {
    val newState = for {
      priceRaw <- message.text
      price <- priceRaw.toLongOption
      draft <- current match {
        case CreateOfferPrice(_, draft) => Some(draft)
        case _                          => None
      }
      updatedDraft = draft.copy(price = Some(price))
    } yield CreateOfferDescription(current, updatedDraft)

    newState.getOrElse(Error(current, "Пожалуйста, введите цену (целое число рублей)")).pure
  }

  private def toCreateOfferPhoto(current: State)(implicit message: Message): F[State] =
    current match {
      case CreateOfferDescription(_, draft) => // description has been uploaded
        val newState = for {
          description <- message.text
          updatedDraft = draft.copy(description = Some(description))
        } yield CreateOfferPhoto(current, updatedDraft)

        newState.getOrElse(Error(current, "Пожалуйста, введите описание к объявлению")).pure

      case CreateOfferPhoto(_, draft) => // another photo has been uploaded
        val newState = for {
          photoWithSizes <- message.photo
          photo <- photoWithSizes.lastOption
          updatedDraft = draft.copy(photos = draft.photos ++ Seq(photo.fileId))
        } yield CreateOfferPhoto(current, updatedDraft)

        newState.getOrElse(Error(current, "Пожалуйста, загрузите фото")).pure

      case _ =>
        Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toCreatedOffer(current: State)(implicit message: Message): F[State] =
    current match {
      case CreateOfferPhoto(_, draft) => CreatedOffer(current, draft).pure
      case _                          => Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toMyOffers(current: State)(implicit message: Message): F[State] =
    MyOffers(current, getOffers(5)).pure

  private def getOffers(maxLength: Int): Seq[Stub] =
    (0 until Random.nextInt(maxLength)).map { index =>
      Stub(UUID.randomUUID(), s"${Random.nextString(Random.nextInt(30))} ($index)")
    }

  private def toMyOffer(current: State)(implicit message: Message): F[State] = {
    val optOffer = for {
      offerId <- message.text
      offers <- current match {
        case MyOffers(_, offers) => Some(offers)
        case _                   => None
      }
      offer <- offers.find(_.id == UUID.fromString(offerId))
    } yield offer

    optOffer match {
      case Some(offer) => MyOffer(current, offer).pure
      case None        => Error(current, "К сожалению, такого id не существует! Попробуйте ещё раз").pure
    }
  }

  private def toEditOffer(current: State)(implicit message: Message): F[State] =
    EditOffer(current).pure

  private def toEditOfferName(current: State)(implicit message: Message): F[State] =
    EditOfferName(current).pure

  private def toEditOfferPrice(current: State)(implicit message: Message): F[State] =
    EditOfferPrice(current).pure

  private def toEditOfferDescription(current: State)(implicit message: Message): F[State] =
    EditOfferDescription(current).pure

  private def toAddOfferPhoto(current: State)(implicit message: Message): F[State] =
    AddOfferPhoto(current).pure

  private def toDeleteOfferPhotos(current: State)(implicit message: Message): F[State] =
    UpdatedOffer(current, "Все фотографии были удалены из объявления").pure

  private def toUpdatedOffer(current: State)(implicit message: Message): F[State] =
    current match {
      case EditOfferName(_) =>
        message.text match {
          case Some(newName) =>
            UpdatedOffer(current, s"Название было изменено на \"$newName\"").pure
          case None =>
            Error(current, "Пожалуйста, введите новое название объявления").pure
        }
      case EditOfferPrice(_) =>
        message.text.flatMap(_.toIntOption) match {
          case Some(newPrice) =>
            UpdatedOffer(current, s"Цена была изменена на $newPrice").pure
          case None =>
            Error(current, "Пожалуйста, введите новую цену (целое число рублей)").pure
        }
      case EditOfferDescription(_) =>
        message.text match {
          case Some(newDescription) =>
            UpdatedOffer(current, s"Описание было изменено").pure
          case None =>
            Error(current, "Пожалуйста, введите новое описание объявления").pure
        }
      case AddOfferPhoto(_) =>
        message.photo.flatMap(_.lastOption) match {
          case Some(newPhoto) =>
            UpdatedOffer(current, s"Фотография была добавлена к объявлению").pure
          case None =>
            Error(current, "Пожалуйста, загрузите фото").pure
        }
      case _ =>
        Error(current, "Произошла ошибка! Попробуйте ещё раз").pure
    }

  private def toDeleteOffer(current: State)(implicit message: Message): F[State] =
    DeletedOffer(current).pure

  private def toBack(current: State)(implicit message: Message): F[State] =
    current match {
      case DeletedOffer(previous) =>
        previous.optPrevious.getOrElse(Started).pure
      case UpdatedOffer(previous, _) =>
        // returns the nearest EditOffer
        previous match {
          case EditOffer(_) => previous.pure
          case _            => previous.optPrevious.getOrElse(Started).pure
        }
      case _ =>
        current.optPrevious.getOrElse(Started).pure
    }

  // internal

  private def toStarted(current: State)(implicit message: Message): F[State] =
    Started.pure
}
