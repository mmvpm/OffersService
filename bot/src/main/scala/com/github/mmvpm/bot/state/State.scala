package com.github.mmvpm.bot.state

import com.github.mmvpm.bot.model.{Button, Draft, Tag}
import com.github.mmvpm.model.Stub

sealed trait State {
  def tag: Tag
  def next: Seq[Tag]
  def optPrevious: Option[State]
  def text: String
}

object State {

  // tags

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
      case CreateOfferNameTag      => "Разместить объявление"
      case CreatedOfferTag         => "Опубликовать объявление"
      case MyOffersTag             => "Посмотреть мои объявления"
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
      case EditOfferName(_)             => UpdatedOfferTag
      case EditOfferPrice(_)            => UpdatedOfferTag
      case EditOfferDescription(_)      => UpdatedOfferTag
      case AddOfferPhoto(_)             => UpdatedOfferTag
      case _                            => UnknownTag
    }

  // previous state

  trait NoPrevious extends State {
    val optPrevious: Option[State] = None
  }

  trait WithPrevious extends State { self: { val previous: State } =>
    val optPrevious: Option[State] = Some(self.previous)
  }

  // beginning

  case object Started extends State with NoPrevious {
    val tag: Tag = StartedTag
    val next: Seq[Tag] = Seq(SearchTag, CreateOfferNameTag, MyOffersTag)
    val text: String = "Как я могу вам помочь?"
  }

  // search

  case class Search(previous: State) extends State with WithPrevious {
    val tag: Tag = SearchTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String = "Что хотите найти?"
  }

  case class Listing(previous: State, offers: Seq[Stub], from: Int) extends State with WithPrevious {

    val tag: Tag = ListingTag

    val next: Seq[Tag] = nextPageTag ++ Seq(BackTag, StartedTag)

    val text: String =
      s"""
        |Вот что нашлось по вашему запросу:
        |
        |${offers.slice(from, from + Listing.StepSize).mkString("- ", "\n- ", "")}
        |
        |Если хотите посмотреть одно подробнее, напишите мне его id
        |""".stripMargin

    private lazy val nextPageTag =
      if (from + Listing.StepSize < offers.length)
        Seq(ListingTag)
      else
        Seq()
  }

  object Listing {

    val StepSize = 5

    def start(previous: State, offers: Seq[Stub]): Listing =
      Listing(previous: State, offers: Seq[Stub], from = 0)
  }

  case class OneOffer(previous: State, offer: Stub) extends State with WithPrevious {
    val tag: Tag = OneOfferTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String =
      s"""
        |Выбранное объявление:
        |
        |- id: ${offer.id}
        |- data: ${offer.data}
        |""".stripMargin
  }

  // create offer

  case class CreateOfferName(previous: State) extends State with WithPrevious {
    val tag: Tag = CreateOfferNameTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String = "Как будет называться ваше объявление?"
  }

  case class CreateOfferPrice(previous: State, draft: Draft) extends State with WithPrevious {
    val tag: Tag = CreateOfferPriceTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String = "Введите цену, за которую вы готовы продать"
  }

  case class CreateOfferDescription(previous: State, draft: Draft) extends State with WithPrevious {
    val tag: Tag = CreateOfferDescriptionTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String = "Добавьте описание к вашему объявлению"
  }

  case class CreateOfferPhoto(previous: State, draft: Draft) extends State with WithPrevious {
    val tag: Tag = CreateOfferPhotoTag
    val next: Seq[Tag] = Seq(CreatedOfferTag, BackTag)
    val text: String = "Добавьте одну или несколько фотографий"
  }

  case class CreatedOffer(previous: State, draft: Draft) extends State with WithPrevious {
    val tag: Tag = CreatedOfferTag
    val next: Seq[Tag] = Seq(StartedTag)
    val text: String = s"Объявление размещено. Вы можете посмотреть его в разделе \"Мои объявления\"\n\n$draft"
  }

  // my offers

  case class MyOffers(previous: State, offers: Seq[Stub]) extends State with WithPrevious {
    val tag: Tag = MyOffersTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String =
      s"""
         |Все ваши объявления:
         |
         |${offers.map(_.id).mkString("-`", "`\n-`", "`")}
         |
         |Если хотите посмотреть одно подробнее, напишите мне его id
         |""".stripMargin
  }

  case class MyOffer(previous: State, offer: Stub) extends State with WithPrevious {
    val tag: Tag = MyOfferTag
    val next: Seq[Tag] = Seq(EditOfferTag, DeletedOfferTag, BackTag)
    val text: String =
      s"""
         |Ваше объявление:
         |
         |- id: ${offer.id}
         |- data: ${offer.data}
         |""".stripMargin
  }

  // edit my offer

  case class EditOffer(previous: State) extends State with WithPrevious {
    val tag: Tag = EditOfferTag
    val next: Seq[Tag] =
      Seq(EditOfferNameTag, EditOfferPriceTag, EditOfferDescriptionTag, AddOfferPhotoTag, DeleteOfferPhotosTag, BackTag)
    val text: String = "Что хотите поменять?"
  }

  case class EditOfferName(previous: State) extends State with WithPrevious {
    val tag: Tag = EditOfferNameTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String = "Введите новое название объявления"
  }

  case class EditOfferPrice(previous: State) extends State with WithPrevious {
    val tag: Tag = EditOfferPriceTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String = "Введите новую цену"
  }

  case class EditOfferDescription(previous: State) extends State with WithPrevious {
    val tag: Tag = EditOfferDescriptionTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String = "Введите новое описание к объявлению"
  }

  case class AddOfferPhoto(previous: State) extends State with WithPrevious {
    val tag: Tag = AddOfferPhotoTag
    val next: Seq[Tag] = Seq(BackTag)
    val text: String = "Загрузите фотографию"
  }

  case class UpdatedOffer(previous: State, text: String) extends State with WithPrevious {
    val tag: Tag = UpdatedOfferTag
    val next: Seq[Tag] = Seq(BackTag)
  }

  // delete my offer

  case class DeletedOffer(previous: State) extends State with WithPrevious {
    val tag: Tag = DeletedOfferTag
    val next: Seq[Tag] = Seq(StartedTag)
    val text: String = "Объявление удалено"
  }

  // error

  case class Error(returnTo: State, message: String) extends State with NoPrevious {
    val tag: Tag = ErrorTag
    val next: Seq[Tag] = Seq()
    val text: String = message
  }
}
