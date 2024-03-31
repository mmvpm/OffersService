package com.github.mmvpm.service.api.support

import com.github.mmvpm.model.{Session, UserID}
import com.github.mmvpm.service.api.SessionHeaderName
import com.github.mmvpm.service.api.error.ApiError
import com.github.mmvpm.service.service.auth.AuthService
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.{Endpoint, PublicEndpoint, auth, header}
import sttp.tapir.server.PartialServerEndpoint

trait AuthSessionSupport[F[_]] {

  def authService: AuthService[F]

  implicit class RichEndpointWithSession(endpoint: PublicEndpoint[Unit, ApiError, Unit, Any]) {
    def withSession: PartialServerEndpoint[Session, UserID, Unit, ApiError, Unit, Any, F] = authorized(endpoint)
  }

  private def authorized(
      endpoint: PublicEndpoint[Unit, ApiError, Unit, Any]
  ): PartialServerEndpoint[Session, UserID, Unit, ApiError, Unit, Any, F] =
    endpoint
      // Use `header[Session]` instead of `cookie[Session]` due to this swagger issue:
      // https://stackoverflow.com/questions/49272171/sending-cookie-session-id-with-swagger-3-0
      .securityIn(auth.apiKey(header[Session](SessionHeaderName), WWWAuthenticateChallenge("session-auth")))
      .serverSecurityLogic[UserID, F](authService.resolveSession(_).value)
}
