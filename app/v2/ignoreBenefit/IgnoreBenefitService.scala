/*
 * Copyright 2026 HM Revenue & Customs
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

package v2.ignoreBenefit

import api.controllers.RequestContext
import api.models.errors.*
import api.services.{BaseService, ServiceOutcome}
import cats.implicits.*
import v2.ignoreBenefit.model.request.IgnoreBenefitRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IgnoreBenefitService @Inject() (connector: IgnoreBenefitConnector) extends BaseService {

  def ignoreBenefit(request: IgnoreBenefitRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {

    connector.ignoreBenefit(request).map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private val downstreamErrorMap: Map[String, MtdError] = {

    val IFErrors = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAX_YEAR"          -> TaxYearFormatError,
      "INVALID_BENEFIT_ID"        -> BenefitIdFormatError,
      "IGNORE_FORBIDDEN"          -> RuleIgnoreForbiddenError,
      "NO_DATA_FOUND"             -> NotFoundError,
      "NOT_SUPPORTED_TAX_YEAR"    -> RuleTaxYearNotEndedError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError,
      "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
      "SERVICE_ERROR"             -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

    val HIPErrors = Map(
      "1215" -> NinoFormatError,
      "1117" -> TaxYearFormatError,
      "1231" -> BenefitIdFormatError,
      "1232" -> RuleIgnoreForbiddenError,
      "5010" -> NotFoundError,
      "1115" -> RuleTaxYearNotEndedError,
      "5000" -> RuleTaxYearNotSupportedError,
      "4200" -> RuleOutsideAmendmentWindow,
      "1216" -> InternalError
    )

    IFErrors ++ HIPErrors

  }

}
