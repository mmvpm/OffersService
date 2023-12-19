package com.github.mmvpm.nemia.dao.session

import cats.data.EitherT
import com.github.mmvpm.model.{Session, UserID}
import com.github.mmvpm.nemia.dao.error.SessionDaoError

trait SessionDao[F[_]] {
  def getUserId(session: Session): EitherT[F, SessionDaoError, UserID]
  def saveSession(userId: UserID, session: Session): EitherT[F, SessionDaoError, Unit]
}
