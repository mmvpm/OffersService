package com.github.mmvpm.nemia.dao.session

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import com.github.mmvpm.nemia.dao.error.SessionNotFoundDaoError
import com.github.mmvpm.nemia.dao.session.{SessionDao, SessionDaoRedis}
import com.redis.RedisClient
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock

import java.util.UUID

//noinspection TypeAnnotation
class SessionDaoRedisSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  "SessionDao" - {
    "save session" in {
      val redis = mock[RedisClient]
      val sessionDao = new SessionDaoRedis[IO](redis, 0)

      val userId = UUID.randomUUID()
      val session = UUID.randomUUID()

      when(redis.setex(any, any, any)(any)).thenReturn(true)

      sessionDao.saveSession(userId, session).value.asserting(_.isRight shouldBe true)
    }
    "get saved session" in {
      val redis = mock[RedisClient]
      val sessionDao = new SessionDaoRedis[IO](redis, 0)

      val userId = UUID.randomUUID()
      val session = UUID.randomUUID()

      when(redis.setex(any, any, any)(any)).thenReturn(true)
      when(redis.get[String](any)(any, any)).thenReturn(Some(userId.toString))

      sessionDao.getUserId(session).value.asserting(_ shouldBe Right(userId))
    }
    "return not found" in {
      val redis = mock[RedisClient]
      val sessionDao = new SessionDaoRedis[IO](redis, 0)

      val session = UUID.randomUUID()

      when(redis.get(any)(any, any)).thenReturn(None)

      sessionDao.getUserId(session).value.asserting(_ shouldBe Left(SessionNotFoundDaoError(session)))
    }
  }
}
