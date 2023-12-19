//noinspection TypeAnnotation
package com.github.mmvpm.nemia.service.auth

import cats.data.EitherT
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{std, IO}
import cats.effect.std.UUIDGen
import com.github.mmvpm.model.{UserID, UserStatus}
import com.github.mmvpm.nemia.api.error.InvalidSessionApiError
import com.github.mmvpm.nemia.api.response.SessionResponse
import com.github.mmvpm.nemia.dao.error.{SessionDaoError, SessionNotFoundDaoError}
import com.github.mmvpm.nemia.dao.session.SessionDao
import com.github.mmvpm.nemia.dao.user.UserDao
import com.github.mmvpm.nemia.service.auth.AuthServiceImpl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock

import java.util.UUID

class AuthServiceImplSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Fixture {

  "AuthService" - {
    "create and return a new session" in {
      val userId = UUID.randomUUID()
      val session = UUID.randomUUID()

      when(uuidGen.randomUUID).thenReturn(IO.pure(session))
      when(sessionDao.saveSession(any, any)).thenReturn(EitherT.pure(()))

      authService.getSession(userId).value.asserting(_ shouldBe Right(SessionResponse(session)))
    }
    "return existing session" in {
      val userId = UUID.randomUUID()
      val session = UUID.randomUUID()

      when(sessionDao.getUserId(any)).thenReturn(EitherT.pure(userId))
      when(userDao.getUserStatus(any)).thenReturn(EitherT.pure(UserStatus.Active))

      authService.resolveSession(session).value.asserting(_ shouldBe Right(userId))
    }
    "fail on invalid session" in {
      val session = UUID.randomUUID()

      when(sessionDao.getUserId(any)).thenReturn(EitherT.fromEither(Left(SessionNotFoundDaoError(session))))

      authService.resolveSession(session).value.asserting(_ shouldBe Left(InvalidSessionApiError(session)))
    }
  }
}

trait Fixture {
  implicit val uuidGen: UUIDGen[IO] = mock[UUIDGen[IO]]
  val userDao = mock[UserDao[IO]]
  val sessionDao = mock[SessionDao[IO]]
  val authService = new AuthServiceImpl[IO](userDao, sessionDao, 0L)
}
