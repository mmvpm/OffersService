package com.github.mmvpm.service.api

import com.github.mmvpm.model.{OfferID, Session, UserID}
import io.circe.Decoder.Result
import sttp.model.StatusCode
import sttp.tapir.{FieldName, Schema, SchemaType}
import sttp.tapir.SchemaType.{SInteger, SProduct, SProductField, SString}

package object error {

  sealed trait ApiError {
    def id: String
    def code: StatusCode
    def details: String
  }

  trait BadRequestApiError extends ApiError {
    def code: StatusCode = StatusCode.BadRequest
  }

  trait UnauthorizedApiError extends ApiError {
    def code: StatusCode = StatusCode.Unauthorized
  }

  trait NotFoundApiError extends ApiError {
    def code: StatusCode = StatusCode.NotFound
  }

  trait InternalApiError extends ApiError {
    def code: StatusCode = StatusCode.InternalServerError
  }

  // session

  case class InvalidSessionApiError(session: Session) extends UnauthorizedApiError {
    val id: String = "session.invalid"
    val details: String = s"session $session has expired or is invalid"
  }

  case class SessionDaoInternalApiError(message: String) extends InternalApiError {
    val id: String = "session.internal.error"
    val details: String = s"session dao internal error: $message"
  }

  // user

  case class UserNotFoundApiError(userId: UserID) extends NotFoundApiError {
    val id: String = "user.not.found"
    val details: String = s"user $userId is not found"
  }

  case class UserLoginNotFoundApiError(login: String) extends NotFoundApiError {
    val id: String = "user.not.found"
    val details: String = s"user @$login is not found"
  }

  case class UserAlreadyExistsApiError(login: String) extends BadRequestApiError {
    val id: String = "user.already.exists"
    val details: String = s"user @$login already exists"
  }

  case class WrongPasswordApiError(password: String) extends BadRequestApiError {
    val id: String = "user.wrong.password"
    val details: String = s"incorrect password '$password'"
  }

  case class UserValidationApiError(details: String) extends BadRequestApiError {
    val id: String = "user.validation.failed"
  }

  case class UserDaoInternalApiError(message: String) extends InternalApiError {
    val id: String = "user.internal.error"
    val details: String = s"user dao internal error: $message"
  }

  // offer

  case class OfferNotFoundApiError(offerId: OfferID) extends NotFoundApiError {
    val id: String = "offer.not.found"
    val details: String = s"offer $offerId is not found"
  }

  case class OfferDaoInternalApiError(message: String) extends InternalApiError {
    val id: String = "offer.dao.internal.error"
    val details: String = s"offer dao internal error: $message"
  }

  // schema

  object SchemaInstances {

    implicit val schemaDefault: Schema[ApiError] = schema(code = 400).description("error")
    implicit val schema400: Schema[BadRequestApiError] = schema(code = 400).description("bad request")
    implicit val schema401: Schema[UnauthorizedApiError] = schema(code = 401).description("unauthorized")
    implicit val schema404: Schema[NotFoundApiError] = schema(code = 404).description("not found")
    implicit val schema500: Schema[InternalApiError] = schema(code = 500).description("internal server error")

    private def schemaCode(code: Int) = Schema(SInteger[AnyVal]()).format("int32").default(code)

    private def schema[Error <: ApiError](code: Int): Schema[Error] = Schema(
      schemaType = SProduct(
        List(
          SProductField(FieldName("id"), Schema.schemaForString, e => Some(e.id)),
          SProductField(FieldName("code"), schemaCode(code), e => Some(e.code)),
          SProductField(FieldName("details"), Schema.schemaForString, e => Some(e.details))
        )
      )
    )
  }

  // circe

  object CirceInstances {

    import io.circe._

    implicit def decoder[Error <: ApiError]: Decoder[Error] = (c: HCursor) =>
      for {
        idField <- c.downField("id").as[String]
        codeField <- c.downField("code").as[Int]
        detailsField <- c.downField("details").as[String]
      } yield new ApiError {
        val id: String = idField
        val code: StatusCode = StatusCode(codeField)
        val details: String = detailsField
      }.asInstanceOf[Error]

    implicit def encoder[Error <: ApiError]: Encoder[Error] = (e: Error) =>
      Json.obj(
        "id" -> Json.fromString(e.id),
        "code" -> Json.fromInt(e.code.code),
        "details" -> Json.fromString(e.details)
      )

    implicit val decoderDefault: Decoder[ApiError] = decoder[ApiError]
    implicit val encoderDefault: Encoder[ApiError] = encoder[ApiError]

    implicit val decoder400: Decoder[BadRequestApiError] = decoder[BadRequestApiError]
    implicit val encoder400: Encoder[BadRequestApiError] = encoder[BadRequestApiError]

    implicit val decoder401: Decoder[UnauthorizedApiError] = decoder[UnauthorizedApiError]
    implicit val encoder401: Encoder[UnauthorizedApiError] = encoder[UnauthorizedApiError]

    implicit val decoder404: Decoder[NotFoundApiError] = decoder[NotFoundApiError]
    implicit val encoder404: Encoder[NotFoundApiError] = encoder[NotFoundApiError]

    implicit val decoder500: Decoder[InternalApiError] = decoder[InternalApiError]
    implicit val encoder500: Encoder[InternalApiError] = encoder[InternalApiError]
  }
}
