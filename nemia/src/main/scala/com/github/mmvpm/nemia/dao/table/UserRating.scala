package com.github.mmvpm.nemia.dao.table

import com.github.mmvpm.model.{Mark, Rating, User, UserID}

case class UserRating(fromUserId: UserID, toUserId: UserID, mark: Int)

object UserRating {

  def from(user: User): List[UserRating] =
    user.rating.marks.map { mark =>
      UserRating(mark.fromId, mark.toId, mark.mark)
    }

  def mergeTo(user: User, userRating: List[UserRating]): User = {
    val newRating = userRating.map(rating => Mark(rating.fromUserId, rating.toUserId, rating.mark))
    user.copy(rating = Rating(newRating))
  }
}
