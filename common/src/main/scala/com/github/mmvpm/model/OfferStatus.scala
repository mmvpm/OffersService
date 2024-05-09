package com.github.mmvpm.model

//noinspection TypeAnnotation
object OfferStatus extends Enumeration {
  type OfferStatus = Value
  val Active = Value("Active")
  val OnModeration = Value("OnModeration")
  val Banned = Value("Banned")
  val Deleted = Value("Deleted")

  implicit class RichStatus(status: OfferStatus) {

    def isActive: Boolean = status match {
      case Active | OnModeration => true
      case Banned | Deleted      => false
    }

    def visibleToOwner: Boolean = status match {
      case Deleted => false
      case _       => true
    }
  }
}
