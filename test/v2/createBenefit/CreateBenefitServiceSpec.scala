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

package v2.createBenefit

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.{ServiceOutcome, ServiceSpec}
import v2.createBenefit.def1.model.request.{Def1_CreateBenefitRequestBody, Def1_CreateBenefitRequestData}
import v2.createBenefit.model.response.CreateBenefitResponse
import v2.models.domain.BenefitType

import scala.concurrent.Future

class CreateBenefitServiceSpec extends ServiceSpec {

  private val nino    = "AA112233A"
  private val taxYear = "2021-22"

  private val createBenefitRequestBody =
    Def1_CreateBenefitRequestBody(BenefitType.incapacityBenefit.toString, "2020-08-03", Some("2020-12-03"))

  private val request = Def1_CreateBenefitRequestData(Nino(nino), TaxYear.fromMtd(taxYear), createBenefitRequestBody)

  private val response = CreateBenefitResponse("b1e8057e-fbbc-47a8-a8b4-78d9f015c253")

  "CreateBenefitService" when {
    "createBenefit" must {
      "return correct result for a success" in new Test {
        val outcome: Right[Nothing, ResponseWrapper[CreateBenefitResponse]] = Right(ResponseWrapper(correlationId, response))

        MockCreateBenefitConnector
          .createBenefit(request)
          .returns(Future.successful(outcome))

        val result: ServiceOutcome[CreateBenefitResponse] = await(service.createBenefit(request))
        result shouldBe outcome
      }

      "map errors according to spec" when {
        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockCreateBenefitConnector
              .createBenefit(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            val result: ServiceOutcome[CreateBenefitResponse] = await(service.createBenefit(request))
            result shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val errors = List(
          "INVALID_TAXABLE_ENTITY_ID"   -> NinoFormatError,
          "INVALID_TAX_YEAR"            -> TaxYearFormatError,
          "INVALID_CORRELATIONID"       -> InternalError,
          "INVALID_PAYLOAD"             -> InternalError,
          "BENEFIT_TYPE_ALREADY_EXISTS" -> RuleBenefitTypeExists,
          "NOT_SUPPORTED_TAX_YEAR"      -> RuleTaxYearNotEndedError,
          "INVALID_START_DATE"          -> RuleStartDateAfterTaxYearEndError,
          "INVALID_CESSATION_DATE"      -> RuleEndDateBeforeTaxYearStartError,
          "OUTSIDE_AMENDMENT_WINDOW"    -> RuleOutsideAmendmentWindow,
          "SERVER_ERROR"                -> InternalError,
          "SERVICE_UNAVAILABLE"         -> InternalError
        )

        errors.foreach(serviceError.tupled)
      }
    }
  }

  private trait Test extends MockCreateBenefitConnector {
    val service: CreateBenefitService = new CreateBenefitService(mockCreateBenefitConnector)
  }

}
