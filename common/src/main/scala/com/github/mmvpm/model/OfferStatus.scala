package com.github.mmvpm.model

//noinspection TypeAnnotation
object OfferStatus extends Enumeration {
  type OfferStatus = Value
  val Active = Value("Active")
  val Banned = Value("Banned")
  val Deleted = Value("Deleted")
}
