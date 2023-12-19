package com.github.mmvpm.nemia.dao

sealed trait DaoUpdate[+T]

object DaoUpdate {
  case object DoNothing extends DaoUpdate[Nothing]
  case class SaveNew[T](newValue: T) extends DaoUpdate[T]
}
