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

package v2.unignoreBenefit

import api.controllers.RequestContext
import api.models.errors.*
import api.services.{BaseService, ServiceOutcome}
import cats.implicits.*
import v2.unignoreBenefit.model.request.UnignoreBenefitRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnignoreBenefitService @Inject() (connector: UnignoreBenefitConnector) extends BaseService {

  def unignoreBenefit(request: UnignoreBenefitRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {
    connector.unignoreBenefit(request).map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private val downstreamErrorMap: Map[String, MtdError] = {

    val IFSErrors: Map[String, MtdError] = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAX_YEAR"          -> TaxYearFormatError,
      "INVALID_BENEFIT_ID"        -> BenefitIdFormatError,
      "CUSTOMER_ADDED"            -> RuleUnignoreForbiddenError,
      "NO_DATA_FOUND"             -> NotFoundError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError,
      "BEFORE_TAX_YEAR_ENDED"     -> RuleTaxYearNotEndedError,
      "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
      "SERVICE_ERROR"             -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

    val HipErrors: Map[String, MtdError] = Map(
      "1215" -> NinoFormatError,
      "1117" -> TaxYearFormatError,
      "1231" -> BenefitIdFormatError,
      "1233" -> RuleUnignoreForbiddenError,
      "5000" -> RuleTaxYearNotSupportedError,
      "5010" -> NotFoundError,
      "1115" -> RuleTaxYearNotEndedError,
      "4200" -> RuleOutsideAmendmentWindow,
      "1216" -> InternalError,
      "5009" -> InternalError
    )
    IFSErrors ++ HipErrors
  }

}
