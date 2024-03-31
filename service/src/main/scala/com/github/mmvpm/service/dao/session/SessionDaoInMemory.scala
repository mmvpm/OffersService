package com.github.mmvpm.service.dao.session

import cats.Applicative
import cats.data.EitherT
import cats.effect.Sync
import com.github.mmvpm.model.{Session, UserID}
import com.github.mmvpm.service.dao.error._

import scala.collection.mutable

class SessionDaoInMemory[F[_]: Applicative: Sync] extends SessionDao[F] {

  override def getUserId(session: Session): EitherT[F, SessionDaoError, UserID] =
    EitherT.fromOption(storage.get(session), SessionNotFoundDaoError(session))

  override def saveSession(userId: UserID, session: Session): EitherT[F, SessionDaoError, Unit] =
    EitherT.liftF(Sync[F].delay(storage.put(session, userId)))

  private val storage: mutable.Map[Session, UserID] = new mutable.HashMap()
}
