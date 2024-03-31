package com.github.mmvpm.service.dao.session

import cats.data.EitherT
import com.github.mmvpm.model.{Session, UserID}
import com.github.mmvpm.service.dao.error.SessionDaoError

trait SessionDao[F[_]] {
  def getUserId(session: Session): EitherT[F, SessionDaoError, UserID]
  def saveSession(userId: UserID, session: Session): EitherT[F, SessionDaoError, Unit]
}
