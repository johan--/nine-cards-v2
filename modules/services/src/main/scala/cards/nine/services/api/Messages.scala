package cards.nine.services.api

import cards.nine.models.{LoginV1Device, UserV1}

case class RequestConfigV1(deviceId: String, token: String, marketToken: Option[String])

case class RequestConfig(
  apiKey: String,
  sessionToken: String,
  androidId: String,
  marketToken: Option[String] = None)

case class LoginResponse(
  statusCode: Int,
  apiKey: String,
  sessionToken: String)

case class LoginResponseV1(
  statusCode: Int,
  userId: Option[String],
  sessionToken: Option[String],
  email: Option[String],
  devices: Seq[LoginV1Device])

trait UserV1Response {
  def statusCode: Int
  def userConfig: UserV1
}

case class GetUserV1Response(
  statusCode: Int,
  userConfig: UserV1) extends UserV1Response

