package com.github.mmvpm.nemia.api

import cats.Monad
import com.github.mmvpm.nemia.api.request.SignUpRequest
import com.github.mmvpm.nemia.api.response.{SessionResponse, UserIdResponse, UserResponse}
import com.github.mmvpm.nemia.api.util.CirceInstances._
import com.github.mmvpm.nemia.api.util.SchemaInstances._
import com.github.mmvpm.nemia.service.auth.AuthService
import com.github.mmvpm.nemia.service.user.UserService
import com.github.mmvpm.model.{Session, UserID}
import com.github.mmvpm.nemia.api.support.{ApiErrorSupport, AuthBasicSupport}
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.model.UsernamePassword
import sttp.tapir.server.ServerEndpoint

class AuthHandler[F[_]: Monad](override val authService: AuthService[F], userService: UserService[F])
    extends Handler[F]
    with AuthBasicSupport[F]
    with ApiErrorSupport {

  private val signUp: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.post
      .summary("Зарегистрировать нового пользователя")
      .in("api" / "v1" / "auth" / "sign-up")
      .in(jsonBody[SignUpRequest])
      .out(jsonBody[UserResponse])
      .serverLogic(userService.createUser(_).value)

  private val signIn: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.withLoginPassword.post
      .summary("Обменять логин и пароль на сессию")
      .in("api" / "v1" / "auth" / "sign-in")
      .out(jsonBody[SessionResponse])
      .serverLogic(userId => _ => authService.getSession(userId).value)

  private val whoAmI: ServerEndpoint[Any, F] =
    endpoint.withApiErrors.get
      .summary("Получить ID пользователя по сессии")
      .in("api" / "v1" / "auth" / "whoami" / path[Session]("session"))
      .out(jsonBody[UserIdResponse])
      .serverLogic(authService.whoami(_).value)

  override def endpoints: List[ServerEndpoint[Any, F]] =
    List(signUp, signIn, whoAmI).map(_.withTag("auth"))
}
