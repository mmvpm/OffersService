//noinspection TypeAnnotation
package com.github.mmvpm.nemia.service.user

import cats.data.EitherT
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import cats.effect.std.Random
import com.github.mmvpm.model._
import com.github.mmvpm.nemia.api.error.UserNotFoundApiError
import com.github.mmvpm.nemia.api.request.SignUpRequest
import com.github.mmvpm.nemia.api.response.{ApiUser, ApiUserDescription, UserResponse}
import com.github.mmvpm.nemia.dao.error.UserNotFoundDaoError
import com.github.mmvpm.nemia.dao.user.UserDao
import com.github.mmvpm.nemia.service.user.UserServiceImpl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.Instant.now
import java.util.UUID

class UserServiceImplSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Fixture {

  "OfferService" - {
    "return existing user" in {
      val user = createUser()

      when(userDao.getUser(user.id)).thenReturn(EitherT.pure(user))

      userService.getUser(user.id).value.asserting(_ shouldBe Right(UserResponse(ApiUser.from(user))))
    }
    "return not found on non-existent user" in {
      val userId = UUID.randomUUID()

      when(userDao.getUser(userId)).thenReturn(EitherT.fromEither(Left(UserNotFoundDaoError(userId))))

      userService.getUser(userId).value.asserting(_ shouldBe Left(UserNotFoundApiError(userId)))
    }
    "create a new user" in {
      val descriptionRaw = UserDescriptionRaw("login", "password")
      val request = SignUpRequest(descriptionRaw)
      val salt = "salt"

      when(random.nextString(any: Int)).thenReturn(IO.pure(salt))
      when(userDao.createUser(any)).thenReturn(EitherT.pure(()))

      userService.createUser(request).value.asserting { result =>
        result.map(_.user.description) shouldBe Right(ApiUserDescription.from(descriptionRaw.encrypt(salt)))
      }
    }
  }

  private def createUser(): User = {
    val selfId = UUID.randomUUID()
    User(
      id = selfId,
      description = UserDescription(
        login = "login",
        password = PasswordHashed("hash", "salt"),
        email = Some("email@gmail.com"),
        phone = Some("+79001112233")
      ),
      status = UserStatus.Active,
      rating = Rating(marks = List(Mark(UUID.randomUUID(), selfId, 9))),
      registeredAt = now
    )
  }
}

trait Fixture {
  val random = mock[Random[IO]]
  val userDao = mock[UserDao[IO]]
  val userService = new UserServiceImpl[IO](userDao, random)
}
