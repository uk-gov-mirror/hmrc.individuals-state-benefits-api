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

package v2.listBenefits

import api.controllers.RequestContext
import api.models.errors.*
import api.services.{BaseService, ServiceOutcome}
import cats.implicits.*
import v2.listBenefits.model.request.ListBenefitsRequestData
import v2.listBenefits.model.response.{CustomerStateBenefit, HMRCStateBenefit, ListBenefitsResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListBenefitsService @Inject() (connector: ListBenefitsConnector) extends BaseService {

  def listBenefits(request: ListBenefitsRequestData)(implicit
      ctx: RequestContext,
      ec: ExecutionContext): Future[ServiceOutcome[ListBenefitsResponse[HMRCStateBenefit, CustomerStateBenefit]]] = {

    connector.listBenefits(request).map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private def downstreamErrorMap: Map[String, MtdError] = {

    val errors = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAX_YEAR"          -> TaxYearFormatError,
      "INVALID_BENEFIT_ID"        -> BenefitIdFormatError,
      "INVALID_VIEW"              -> InternalError,
      "NO_DATA_FOUND"             -> NotFoundError,
      "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

    val extraTysErrors: Map[String, MtdError] = Map(
      "NOT_FOUND" -> NotFoundError
    )

    errors ++ extraTysErrors
  }

}
