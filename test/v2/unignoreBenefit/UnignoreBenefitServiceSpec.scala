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

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.{ServiceOutcome, ServiceSpec}
import v2.models.domain.BenefitId
import v2.unignoreBenefit.def1.model.request.Def1_UnignoreBenefitRequestData

import scala.concurrent.Future

class UnignoreBenefitServiceSpec extends ServiceSpec {

  private val nino      = "AA111111A"
  private val taxYear   = "2019-20"
  private val benefitId = "123e4567-e89b-12d3-a456-426614174000"

  private val request = Def1_UnignoreBenefitRequestData(Nino(nino), TaxYear.fromMtd(taxYear), BenefitId(benefitId))

  "UnignoreBenefitService" when {
    "unignoreBenefit" must {
      "return correct result for a success" in new Test {
        val outcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        MockUnignoreBenefitConnector
          .unignoreBenefit(request)
          .returns(Future.successful(outcome))

        val result: ServiceOutcome[Unit] = await(service.unignoreBenefit(request))
        result shouldBe outcome
      }

      "map errors according to spec" when {
        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockUnignoreBenefitConnector
              .unignoreBenefit(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            val result: ServiceOutcome[Unit] = await(service.unignoreBenefit(request))
            result shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val errors = List(
          "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
          "INVALID_TAX_YEAR"          -> TaxYearFormatError,
          "INVALID_CORRELATION_ID"    -> InternalError,
          "INVALID_BENEFIT_ID"        -> BenefitIdFormatError,
          "CUSTOMER_ADDED"            -> RuleUnignoreForbiddenError,
          "NO_DATA_FOUND"             -> NotFoundError,
          "TAX_YEAR_NOT_SUPPORTED"    -> RuleTaxYearNotSupportedError,
          "BEFORE_TAX_YEAR_ENDED"     -> RuleTaxYearNotEndedError,
          "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
          "SERVICE_ERROR"             -> InternalError,
          "SERVICE_UNAVAILABLE"       -> InternalError,
          ("1215", NinoFormatError),
          ("1117", TaxYearFormatError),
          ("1231", BenefitIdFormatError),
          ("1233", RuleUnignoreForbiddenError),
          ("5000", RuleTaxYearNotSupportedError),
          ("5010", NotFoundError),
          ("1115", RuleTaxYearNotEndedError),
          ("1216", InternalError),
          ("5009", InternalError)
        )

        errors.foreach(serviceError.tupled)
      }
    }
  }

  private trait Test extends MockUnignoreBenefitConnector {
    val service: UnignoreBenefitService = new UnignoreBenefitService(mockUnignoreBenefitConnector)

  }

}
