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

package v2.amendBenefit

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.{ServiceOutcome, ServiceSpec}
import v2.amendBenefit.def1.model.request.{Def1_AmendBenefitRequestBody, Def1_AmendBenefitRequestData}
import v2.models.domain.BenefitId

import scala.concurrent.Future

class AmendBenefitServiceSpec extends ServiceSpec {

  private val nino      = "AA123456A"
  private val taxYear   = "2021-22"
  private val benefitId = "123e4567-e89b-12d3-a456-426614174000"

  private val amendBenefitRequestBody = Def1_AmendBenefitRequestBody("2020-08-03", Some("2020-12-03"))

  private val requestData = Def1_AmendBenefitRequestData(Nino(nino), TaxYear.fromMtd(taxYear), BenefitId(benefitId), amendBenefitRequestBody)

  "AmendBenefitService" when {
    "amendBenefit" must {
      "return correct result for a success" in new Test {
        val outcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        MockAmendBenefitConnector
          .amendBenefit(requestData)
          .returns(Future.successful(outcome))

        val result: ServiceOutcome[Unit] = await(service.amendBenefit(requestData))
        result shouldBe outcome
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockAmendBenefitConnector
            .amendBenefit(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          val result: ServiceOutcome[Unit] = await(service.amendBenefit(requestData))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_TAX_YEAR"          -> TaxYearFormatError,
        "INVALID_BENEFIT_ID"        -> BenefitIdFormatError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "INVALID_PAYLOAD"           -> InternalError,
        "UPDATE_FORBIDDEN"          -> RuleUpdateForbiddenError,
        "NO_DATA_FOUND"             -> NotFoundError,
        "INVALID_START_DATE"        -> RuleStartDateAfterTaxYearEndError,
        "INVALID_CESSATION_DATE"    -> RuleEndDateBeforeTaxYearStartError,
        "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      errors.foreach(serviceError.tupled)
    }
  }

  private trait Test extends MockAmendBenefitConnector {
    val service: AmendBenefitService = new AmendBenefitService(mockAmendBenefitConnector)
  }

}
