package com.github.mmvpm.service.api.support

import com.github.mmvpm.model.UserID
import com.github.mmvpm.service.api.error.ApiError
import com.github.mmvpm.service.service.auth.AuthService
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.model.UsernamePassword
import sttp.tapir.server.PartialServerEndpoint
import sttp.tapir.{PublicEndpoint, auth}

trait AuthBasicSupport[F[_]] {

  def authService: AuthService[F]

  implicit class RichEndpointWithBasicAuth(endpoint: PublicEndpoint[Unit, ApiError, Unit, Any]) {

    def withLoginPassword: PartialServerEndpoint[UsernamePassword, UserID, Unit, ApiError, Unit, Any, F] =
      authorized(endpoint)
  }

  private def authorized(
      endpoint: PublicEndpoint[Unit, ApiError, Unit, Any]
  ): PartialServerEndpoint[UsernamePassword, UserID, Unit, ApiError, Unit, Any, F] =
    endpoint
      .securityIn(auth.basic[UsernamePassword](WWWAuthenticateChallenge.basic("basic-auth")))
      .serverSecurityLogic[UserID, F] { credentials =>
        authService.checkPassword(credentials.username, credentials.password.getOrElse("")).value
      }
}
