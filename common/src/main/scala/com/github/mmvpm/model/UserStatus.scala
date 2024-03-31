package com.github.mmvpm.model

//noinspection TypeAnnotation
object UserStatus extends Enumeration {
  type UserStatus = Value
  val Active = Value("Active")
  val Banned = Value("Banned")
  val Deleted = Value("Deleted")
}
