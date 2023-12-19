package com.github.mmvpm.nemia.api

import cats.Functor
import com.github.mmvpm.nemia.api.request.{RateUserRequest, UpdateUserRequest}
import com.github.mmvpm.nemia.api.response.{OkResponse, UserResponse}
import com.github.mmvpm.nemia.api.support.{ApiErrorSupport, AuthSessionSupport}
import com.github.mmvpm.nemia.api.util.CirceInstances._
import com.github.mmvpm.nemia.api.util.SchemaInstances._
import com.github.mmvpm.nemia.service.auth.AuthService
import com.github.mmvpm.nemia.service.user.UserService
import com.github.mmvpm.model.UserID
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint

class UserHandler[F[_]: Functor](userService: UserService[F], override val authService: AuthService[F])
  extends Handler[F]
  with AuthSessionSupport[F]
  with ApiErrorSupport {

  private val getUser: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.get
      .summary("Получить пользователя по его id")
      .in("api" / "v1" / "user" / path[UserID]("user-id"))
      .out(jsonBody[UserResponse])
      .serverLogic(userService.getUser(_).value)

  private val updateUser: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.put
      .summary("Обновить свой профиль")
      .in("api" / "v1" / "user")
      .in(jsonBody[UpdateUserRequest])
      .out(jsonBody[UserResponse])
      .serverLogic(userId => request => userService.updateUser(userId, request).value)

  private val deleteUser: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.delete
      .summary("Удалить свой профиль")
      .in("api" / "v1" / "user")
      .out(jsonBody[OkResponse])
      .serverLogic(userId => _ => userService.deleteUser(userId).value)

  private val rateUser: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.post
      .summary("Поставить оценку другому пользователю")
      .in("api" / "v1" / "user" / path[UserID]("to-user-id"))
      .in(jsonBody[RateUserRequest])
      .out(jsonBody[OkResponse])
      .serverLogic(fromUserId => { case (toUserId, request) =>
        userService.rateUser(fromUserId, toUserId, request).value
      })

  override def endpoints: List[ServerEndpoint[Any, F]] =
    List(getUser, updateUser, deleteUser, rateUser).map(_.withTag("user"))
}
