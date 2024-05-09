package com.github.mmvpm.bot.state

import com.github.mmvpm.bot.model.{Button, Draft, FullOffer, Tag, TgPhoto}
import com.github.mmvpm.bot.util.PriceUtils.priceText
import com.github.mmvpm.model.{Offer, OfferID, OfferStatus, User}

import scala.util.matching.Regex

sealed trait State {
  def tag: Tag
  def next: Seq[Seq[Tag]]
  def optPrevious: Option[State]
  def text: String
}

object State {

  // tags

  val RegisteredTag = "registered"
  val LoggedInTag = "legged-in"
  val EnterPasswordTag = "enter-password"
  val SearchTag: Tag = "search"
  val ListingTag: Tag = "listing"
  val OneOfferTag: Tag = "one-offer"
  val CreateOfferNameTag: Tag = "create-name"
  val CreateOfferPriceTag: Tag = "create-price"
  val CreateOfferDescriptionTag: Tag = "create-description"
  val CreateOfferPhotoTag: Tag = "create-photo"
  val CreatedOfferTag: Tag = "created"
  val MyOffersTag: Tag = "my-offers"
  val MyOfferTag: Tag = "my-offer"
  val EditOfferTag: Tag = "edit"
  val EditOfferNameTag: Tag = "edit-name"
  val EditOfferPriceTag: Tag = "edit-price"
  val EditOfferDescriptionTag: Tag = "edit-description"
  val AddOfferPhotoTag: Tag = "edit-add-photo"
  val DeleteOfferPhotosTag: Tag = "edit-delete-photo" // without related state (UpdatedOffer instead of it)
  val UpdatedOfferTag: Tag = "updated"
  val DeletedOfferTag: Tag = "delete"
  val BackTag: Tag = "back"
  val StartedTag: Tag = "started"
  val ErrorTag: Tag = "error"
  val UnknownTag: Tag = "unknown"

  def buttonBy(tag: Tag): Button =
    tag match {
      case SearchTag               => "Найти товар"
      case ListingTag              => "На следующую страницу"
      case Listing.chooseOne(idx)  => s"${idx.toInt + 1}" // 1-based for user
      case CreateOfferNameTag      => "Разместить объявление"
      case CreatedOfferTag         => "Опубликовать объявление"
      case MyOffersTag             => "Посмотреть мои объявления"
      case MyOffers.chooseOne(idx) => s"${idx.toInt + 1}" // 1-based for user
      case EditOfferTag            => "Изменить это объявление"
      case EditOfferNameTag        => "Название"
      case EditOfferPriceTag       => "Цену"
      case EditOfferDescriptionTag => "Описание"
      case AddOfferPhotoTag        => "Добавить фото"
      case DeleteOfferPhotosTag    => "Удалить все фото"
      case DeletedOfferTag         => "Удалить это объявление"
      case BackTag                 => "Назад"
      case StartedTag              => "Вернуться в начало"
      case UnknownTag              => sys.error(s"buttonBy($tag)")
    }

  def getNextStateTag(current: State): Tag =
    current match {
      case Search(_)                    => ListingTag
      case Listing(_, _, _)             => OneOfferTag
      case MyOffers(_, _)               => MyOfferTag
      case CreateOfferName(_)           => CreateOfferPriceTag
      case CreateOfferPrice(_, _)       => CreateOfferDescriptionTag
      case CreateOfferDescription(_, _) => CreateOfferPhotoTag
      case CreateOfferPhoto(_, _)       => CreateOfferPhotoTag // upload another photo
      case EditOfferName(_, _)          => UpdatedOfferTag
      case EditOfferPrice(_, _)         => UpdatedOfferTag
      case EditOfferDescription(_, _)   => UpdatedOfferTag
      case AddOfferPhoto(_, _)          => UpdatedOfferTag
      case EnterPassword                => LoggedInTag
      case _                            => UnknownTag
    }

  // previous state

  trait NoPrevious extends State {
    val optPrevious: Option[State] = None
  }

  trait WithPrevious extends State { self: { val previous: State } =>
    val optPrevious: Option[State] = Some(self.previous)
  }

  // offer_id

  trait WithOfferID {
    def offerId: OfferID
  }

  // photos

  trait WithPhotos {
    def photos: Seq[TgPhoto]
  }

  // sign-in & sign-up

  case class Registered(password: String) extends State with NoPrevious {
    val tag: Tag = RegisteredTag
    val next: Seq[Seq[Tag]] = Seq(Seq(StartedTag))
    val text: String =
      s"""
        |Вы успешно зарегистрированы на платформе!
        |
        |Ваш пароль: `$password`
        |""".stripMargin
  }

  case class LoggedIn(name: String) extends State with NoPrevious {
    val tag: Tag = LoggedInTag
    val next: Seq[Seq[Tag]] = Seq(Seq(StartedTag))
    val text: String = s"Добро пожаловать, $name!"
  }

  case object EnterPassword extends State with NoPrevious {
    val tag: Tag = EnterPasswordTag
    val next: Seq[Seq[Tag]] = Seq.empty
    val text: String = "Пожалуйста, введите пароль, чтобы войти в свой профиль:"
  }

  // beginning

  case object Started extends State with NoPrevious {
    val tag: Tag = StartedTag
    val next: Seq[Seq[Tag]] = Seq(Seq(SearchTag), Seq(CreateOfferNameTag), Seq(MyOffersTag))
    val text: String = "Чем я могу вам помочь?"
  }

  // search

  case class Search(previous: State) extends State with WithPrevious {
    val tag: Tag = SearchTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))
    val text: String = "Введите поисковый запрос"
  }

  case class Listing(previous: State, offers: Seq[FullOffer], from: Int)
      extends State
      with WithPrevious
      with WithPhotos {

    import Listing._

    private val StepSizeCropped = scala.math.min(StepSize, offers.size - from)

    val tag: Tag = ListingTag

    val next: Seq[Seq[Tag]] = Seq(offersTags, Seq(BackTag) ++ nextPageTag, Seq(StartedTag))

    val text: String =
      if (offers.nonEmpty)
        s"""
          |$summary
          |
          |С помощью $buttonRangeText можно выбрать одно объявление, чтобы посмотреть его подробнее
          |""".stripMargin
      else
        "По вашему запросу ничего не нашлось :("

    private lazy val buttonRangeText =
      if (StepSizeCropped == 1) "кнопки 1" else s"кнопок от 1 до $StepSizeCropped"

    val photos: Seq[TgPhoto] =
      offers
        .slice(from, from + StepSizeCropped)
        .map(offer => TgPhoto.first(offer.offer.photos))

    def get(idx: Int): FullOffer =
      offers.drop(from + idx).head

    private lazy val nextPageTag =
      if (from + StepSize < offers.length)
        Seq(ListingTag)
      else
        Seq()

    private lazy val offersTags: Seq[Tag] =
      (0 until StepSizeCropped).map(idx => s"$ListingTag-$idx")

    private lazy val summary: String =
      offers
        .slice(from, from + StepSizeCropped)
        .zipWithIndex
        .map { case (offer, idx) =>
          s"${idx + 1}. ${offer.offer.description.name} (${priceText(offer.offer.description.price)})"
        }
        .mkString("\n\n")
  }

  object Listing {

    val StepSize = 5

    val chooseOne: Regex = s"$ListingTag-(\\d*)".r

    def start(previous: State, offers: Seq[FullOffer]): Listing =
      Listing(previous, offers, from = 0)
  }

  case class OneOffer(previous: State, offer: FullOffer) extends State with WithPrevious with WithPhotos {
    val tag: Tag = OneOfferTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))

    val text: String =
      s"""
        |${offer.offer.description.name}
        |
        |Цена: ${priceText(offer.offer.description.price)}
        |
        |${offer.offer.description.text}
        |
        |$source
        |""".stripMargin

    val photos: Seq[TgPhoto] =
      offer.offer.photos.take(5).map(TgPhoto.from)

    private lazy val source =
      offer.offer.source match {
        case Some(source) => s"Источник: $source"
        case None         => s"Продавец: ${offer.user.name} (@${offer.user.login})"
      }
  }

  // create offer

  case class CreateOfferName(previous: State) extends State with WithPrevious {
    val tag: Tag = CreateOfferNameTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))
    val text: String = "Как будет называться ваше объявление?"
  }

  case class CreateOfferPrice(previous: State, draft: Draft) extends State with WithPrevious {
    val tag: Tag = CreateOfferPriceTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))
    val text: String = "Введите цену в рублях, за которую вы готовы продать"
  }

  case class CreateOfferDescription(previous: State, draft: Draft) extends State with WithPrevious {
    val tag: Tag = CreateOfferDescriptionTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))
    val text: String = "Добавьте описание к вашему объявлению"
  }

  case class CreateOfferPhoto(previous: State, draft: Draft) extends State with WithPrevious {
    val tag: Tag = CreateOfferPhotoTag
    val next: Seq[Seq[Tag]] = Seq(Seq(CreatedOfferTag), Seq(BackTag))
    val text: String = "Добавьте одну или несколько фотографий"
  }

  case class CreatedOffer(previous: State, draft: Draft) extends State with WithPrevious {
    val tag: Tag = CreatedOfferTag
    val next: Seq[Seq[Tag]] = Seq(Seq(StartedTag))
    val text: String = s"Объявление размещено. Вы можете посмотреть его в разделе \"Мои объявления\""
  }

  // my offers

  case class MyOffers(previous: State, offers: Seq[Offer]) extends State with WithPrevious with WithPhotos {

    import MyOffers._

    private val StepSizeCropped = scala.math.min(StepSize, offers.size)

    val tag: Tag = MyOffersTag

    val next: Seq[Seq[Tag]] = Seq(offersTags, Seq(BackTag))

    val text: String =
      if (summary.nonEmpty)
        s"""
           |Все ваши объявления:
           |
           |$summary
           |
           |С помощью $buttonRangeText можно выбрать одно объявление, чтобы посмотреть его подробнее
           |""".stripMargin
      else
        "У вас пока что нет ни одного объявления"

    private lazy val buttonRangeText =
      if (StepSizeCropped == 1) "кнопки 1" else s"кнопок от 1 до $StepSizeCropped"

    val photos: Seq[TgPhoto] =
      offers
        .take(StepSizeCropped)
        .map(offer => TgPhoto.first(offer.photos))

    def get(idx: Int): Offer =
      offers.drop(idx).head

    private lazy val offersTags: Seq[Button] =
      offers.indices.map(idx => s"$MyOffersTag-$idx")

    private lazy val summary: String =
      offers
        .take(StepSizeCropped)
        .zipWithIndex
        .map { case (offer, idx) =>
          val statusText = offer.status match {
            case OfferStatus.Banned => " [ЗАБАНЕНО]"
            case _                  => ""
          }
          s"${idx + 1}. ${offer.description.name} (${priceText(offer.description.price)}) $statusText"
        }
        .mkString("\n\n")
  }

  object MyOffers {

    val StepSize = 5

    val chooseOne: Regex = s"$MyOffersTag-(\\d*)".r
  }

  case class MyOffer(previous: State, offer: Offer) extends State with WithPrevious with WithOfferID with WithPhotos {
    val offerId: OfferID = offer.id
    val tag: Tag = MyOfferTag
    val next: Seq[Seq[Tag]] = Seq(Seq(EditOfferTag), Seq(DeletedOfferTag), Seq(BackTag))

    val text: String =
      s"""
         |${offer.description.name}
         |
         |Цена: ${priceText(offer.description.price)}
         |
         |${offer.description.text}
         |
         |Источник: ${offer.source.getOrElse("Размещено через telegram-bot")}
         |""".stripMargin

    val photos: Seq[TgPhoto] =
      offer.photos.take(5).map(TgPhoto.from)
  }

  // edit my offer

  case class EditOffer(previous: State, offerId: OfferID) extends State with WithPrevious with WithOfferID {
    val tag: Tag = EditOfferTag

    val next: Seq[Seq[Tag]] =
      Seq(
        Seq(EditOfferNameTag),
        Seq(EditOfferPriceTag),
        Seq(EditOfferDescriptionTag),
        Seq(AddOfferPhotoTag),
        Seq(DeleteOfferPhotosTag),
        Seq(BackTag)
      )

    val text: String = "Что хотите поменять?"
  }

  case class EditOfferName(previous: State, offerId: OfferID) extends State with WithPrevious with WithOfferID {
    val tag: Tag = EditOfferNameTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))
    val text: String = "Введите новое название объявления"
  }

  case class EditOfferPrice(previous: State, offerId: OfferID) extends State with WithPrevious with WithOfferID {
    val tag: Tag = EditOfferPriceTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))
    val text: String = "Введите новую цену"
  }

  case class EditOfferDescription(previous: State, offerId: OfferID) extends State with WithPrevious with WithOfferID {
    val tag: Tag = EditOfferDescriptionTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))
    val text: String = "Введите новое описание к объявлению"
  }

  case class AddOfferPhoto(previous: State, offerId: OfferID) extends State with WithPrevious with WithOfferID {
    val tag: Tag = AddOfferPhotoTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))
    val text: String = "Загрузите фотографию"
  }

  case class UpdatedOffer(previous: State, text: String) extends State with WithPrevious {
    val tag: Tag = UpdatedOfferTag
    val next: Seq[Seq[Tag]] = Seq(Seq(BackTag))
  }

  // delete my offer

  case class DeletedOffer(previous: State) extends State with WithPrevious {
    val tag: Tag = DeletedOfferTag
    val next: Seq[Seq[Tag]] = Seq(Seq(StartedTag))
    val text: String = "Объявление удалено"
  }

  // error

  case class Error(returnTo: State, message: String) extends State with NoPrevious {
    val tag: Tag = ErrorTag
    val next: Seq[Seq[Tag]] = Seq()
    val text: String = message
  }
}
