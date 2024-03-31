package com.github.mmvpm.service.api

import cats.Functor
import com.github.mmvpm.model.UserID
import com.github.mmvpm.service.api.response.{OkResponse, UserResponse}
import com.github.mmvpm.service.api.support.{ApiErrorSupport, AuthSessionSupport}
import com.github.mmvpm.service.api.util.CirceInstances._
import com.github.mmvpm.service.api.util.SchemaInstances._
import com.github.mmvpm.service.service.auth.AuthService
import com.github.mmvpm.service.service.user.UserService
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

  private val deleteUser: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withSession.delete
      .summary("Удалить свой профиль")
      .in("api" / "v1" / "user")
      .out(jsonBody[OkResponse])
      .serverLogic(userId => _ => userService.deleteUser(userId).value)

  override def endpoints: List[ServerEndpoint[Any, F]] =
    List(getUser, deleteUser).map(_.withTag("user"))
}
