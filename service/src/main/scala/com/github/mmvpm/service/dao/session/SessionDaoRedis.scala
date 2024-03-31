package com.github.mmvpm.service.dao.session

import cats.data.EitherT
import cats.effect.Sync
import com.github.mmvpm.service.dao.session.SessionDaoRedis._
import com.github.mmvpm.model.{Session, UserID}
import com.github.mmvpm.service.dao.error._
import com.github.mmvpm.util.EitherUtils.safe
import com.github.mmvpm.util.Logging
import com.redis._

import java.util.UUID
import scala.util.Try

class SessionDaoRedis[F[_]: Sync](redis: RedisClient, SessionExpirationSeconds: Long)
    extends SessionDao[F]
    with Logging {

  override def getUserId(session: Session): EitherT[F, SessionDaoError, UserID] =
    for {
      valueOpt <- redisGet(session)
      value <- EitherT.fromOption(valueOpt, SessionNotFoundDaoError(session))
      optUserId = Try(UUID.fromString(value)).toOption
      userId <- EitherT.fromOption(optUserId, InvalidRedisValueDaoError(value): SessionDaoError)
    } yield userId

  override def saveSession(userId: UserID, session: Session): EitherT[F, SessionDaoError, Unit] =
    for {
      success <- redisSet(userId, session)
      _ <- EitherT.cond[F](success, (), SessionSaveFailedDaoError(session): SessionDaoError)
    } yield ()

  private def redisGet(session: Session): EitherT[F, SessionDaoError, Option[String]] =
    safe(redis.get(session.toRedis)).leftMap { error =>
      log.error(s"get user id by session $session from redis failed", error)
      SessionGetFailedDaoError(session)
    }

  private def redisSet(userId: UserID, session: Session): EitherT[F, SessionDaoError, Boolean] =
    safe(redis.setex(session.toRedis, SessionExpirationSeconds, userId.toString)).leftMap { error =>
      log.error(s"save session $session to redis failed", error)
      SessionSaveFailedDaoError(session)
    }
}

object SessionDaoRedis {

  private val SessionPrefix = "session"

  implicit class RichSession(session: Session) {
    def toRedis: String = s"$SessionPrefix:$session"
  }
}
