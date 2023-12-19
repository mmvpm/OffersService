package com.github.mmvpm.model

case class Rating(marks: List[Mark])

object Rating {
  def empty: Rating = Rating(List.empty)
}
