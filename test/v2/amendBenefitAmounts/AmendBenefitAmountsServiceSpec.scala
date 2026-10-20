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

package v2.amendBenefitAmounts

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.{ServiceOutcome, ServiceSpec}
import v2.amendBenefitAmounts.def1.model.request.{Def1_AmendBenefitAmountsRequestBody, Def1_AmendBenefitAmountsRequestData}
import v2.models.domain.BenefitId

import scala.concurrent.Future

class AmendBenefitAmountsServiceSpec extends ServiceSpec {

  private val nino      = "AA123456A"
  private val taxYear   = "2021-22"
  private val benefitId = "123e4567-e89b-12d3-a456-426614174000"

  private val body = Def1_AmendBenefitAmountsRequestBody(999.99, Some(123.13))

  private val requestData = Def1_AmendBenefitAmountsRequestData(Nino(nino), TaxYear.fromMtd(taxYear), BenefitId(benefitId), body)

  "AmendBenefitAmountsService" when {
    "amendBenefitAmounts" must {
      "return correct result for a success" in new Test {
        val expectedOutcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        MockAmendBenefitAmountsConnector
          .amendBenefitAmounts(requestData)
          .returns(Future.successful(expectedOutcome))

        val result: ServiceOutcome[Unit] = await(service.amendBenefitAmounts(requestData))
        result shouldBe expectedOutcome
      }
    }

    "map errors according to spec" when {
      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockAmendBenefitAmountsConnector
            .amendBenefitAmounts(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          val result: ServiceOutcome[Unit] = await(service.amendBenefitAmounts(requestData))
          result shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = List(
        "INCOME_SOURCE_NOT_FOUND"         -> NotFoundError,
        "INVALID_TAXABLE_ENTITY_ID"       -> NinoFormatError,
        "INVALID_TAX_YEAR"                -> TaxYearFormatError,
        "INVALID_BENEFIT_ID"              -> BenefitIdFormatError,
        "INVALID_CORRELATIONID"           -> InternalError,
        "INVALID_PAYLOAD"                 -> InternalError,
        "INVALID_REQUEST_BEFORE_TAX_YEAR" -> RuleTaxYearNotEndedError,
        "OUTSIDE_AMENDMENT_WINDOW"        -> RuleOutsideAmendmentWindow,
        "SERVER_ERROR"                    -> InternalError,
        "SERVICE_UNAVAILABLE"             -> InternalError
      )

      val extraTysErrors = List(
        ("INVALID_CORRELATION_ID", InternalError),
        ("TAX_YEAR_NOT_SUPPORTED", RuleTaxYearNotSupportedError),
        ("TAX_DEDUCTION_NOT_ALLOWED", RuleTaxDeductionNotAllowedError)
      )

      (errors ++ extraTysErrors).foreach(serviceError.tupled)

    }
  }

  private trait Test extends MockAmendBenefitAmountsConnector {
    val service: AmendBenefitAmountsService = new AmendBenefitAmountsService(mockAmendBenefitAmountsConnector)
  }

}
