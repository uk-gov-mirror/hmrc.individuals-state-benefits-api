/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.models.errors

import play.api.libs.json.{Json, Writes}

case class MtdError(code: String, message: String, paths: Option[Seq[String]] = None)

object MtdError {
  implicit val writes: Writes[MtdError] = Json.writes[MtdError]
}

object CustomMtdError {
  def unapply(arg: MtdError): Option[String] = Some(arg.code)
}

object NinoFormatError extends MtdError("FORMAT_NINO", "The provided NINO is invalid")

object TaxYearFormatError extends MtdError("FORMAT_TAX_YEAR", "The provided tax year is invalid")

object BenefitIdFormatError extends MtdError("FORMAT_BENEFIT_ID", "The provided benefit ID is invalid")

object StartDateFormatError extends MtdError("FORMAT_START_DATE", "The provided start date is invalid")

object EndDateFormatError extends MtdError("FORMAT_END_DATE", "The provided end date is invalid")

object BenefitTypeFormatError extends MtdError("FORMAT_BENEFIT_TYPE", "The provided benefit type is invalid")

// Rule Errors
object RuleTaxYearNotSupportedError extends
  MtdError("RULE_TAX_YEAR_NOT_SUPPORTED", "The specified tax year is not supported. That is, the tax year specified is before the minimum tax year value")

object RuleTaxYearRangeInvalidError
  extends MtdError(code = "RULE_TAX_YEAR_RANGE_INVALID", message = "Tax year range invalid. A tax year range of one year is required")

object RuleTaxYearNotEndedError extends MtdError( code = "RULE_TAX_YEAR_NOT_ENDED", "Tax year not ended")

object RuleEndDateBeforeStartDateError extends
  MtdError("RULE_END_DATE_BEFORE_START_DATE", "The end date cannot be earlier than the start date")

object RuleStartDateAfterTaxYearEndError extends MtdError("RULE_START_DATE_AFTER_TAX_YEAR_END", "The start date cannot be later than the tax year end")

object RuleEndDateBeforeTaxYearStartError extends
  MtdError("RULE_END_DATE_BEFORE_TAX_YEAR_START", "The end date cannot be before the tax year starts")

object RuleIncorrectOrEmptyBodyError extends MtdError("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted")

object RuleDeleteForbiddenError extends MtdError("RULE_DELETE_FORBIDDEN", "A deletion of a HMRC held state benefit is not permitted")

object RuleUpdateForbiddenError extends MtdError("RULE_UPDATE_FORBIDDEN", "An update for a HMRC held benefit is not permitted")
//Standard Errors
object NotFoundError extends MtdError("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found")

object DownstreamError extends MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred")

object BadRequestError extends MtdError("INVALID_REQUEST", "Invalid request")

object BVRError extends MtdError("BUSINESS_ERROR", "Business validation error")

object ServiceUnavailableError extends MtdError("SERVICE_UNAVAILABLE", "Internal server error")

//Authorisation Errors
object UnauthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised")

object InvalidBearerTokenError extends MtdError("UNAUTHORIZED", "Bearer token is missing or not authorized")

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")

object UnsupportedVersionError extends MtdError("NOT_FOUND", "The requested resource could not be found")

object InvalidBodyTypeError extends MtdError("INVALID_BODY_TYPE", "Expecting text/json or application/json body")
