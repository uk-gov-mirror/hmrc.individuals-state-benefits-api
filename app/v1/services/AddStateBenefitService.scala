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

package v1.services

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging
import v1.connectors.AddStateBenefitConnector
import v1.controllers.EndpointLogContext
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.addStateBenefit.AddStateBenefitRequest
import v1.models.response.AddStateBenefitsResponse
import v1.support.DesResponseMappingSupport

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddStateBenefitService @Inject()(connector: AddStateBenefitConnector)
  extends DesResponseMappingSupport with Logging {

  def addStateBenefit(request: AddStateBenefitRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    logContext: EndpointLogContext): Future[Either[ErrorWrapper, ResponseWrapper[AddStateBenefitsResponse]]] = {

    val result = for {
      desResponseWrapper <- EitherT(connector.addStateBenefit(request)).leftMap(mapDesErrors(desErrorMap))
    } yield desResponseWrapper

    result.value
  }

  private def desErrorMap: Map[String, MtdError] =
    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAX_YEAR" -> TaxYearFormatError,
      "INVALID_CORRELATIONID" -> DownstreamError,
      "INVALID_PAYLOAD" -> DownstreamError,
      "INVALID_REQUEST_TAX_YEAR" -> RuleTaxYearNotSupportedError,
      "NOT_SUPPORTED_TAX_YEAR" -> RuleTaxYearNotEndedError,
      "INVALID_START_DATE" -> RuleStartDateAfterTaxYearEndError,
      "INVALID_CESSATION_DATE" -> RuleEndDateBeforeTaxYearStartError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    )
}