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

package v2.deleteBenefitAmounts

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.{ServiceOutcome, ServiceSpec}
import v2.deleteBenefitAmounts.def1.model.request.Def1_DeleteBenefitAmountsRequestData
import v2.models.domain.BenefitId

import scala.concurrent.Future

class DeleteBenefitAmountsServiceSpec extends ServiceSpec {

  private val request =
    Def1_DeleteBenefitAmountsRequestData(Nino("AA112233A"), TaxYear.fromMtd("2023-24"), BenefitId("b1e8057e-fbbc-47a8-a8b4-78d9f015c253"))

  "DeleteOtherEmploymentIncomeServiceSpec" when {
    "the downstream request is successful" must {
      "return a success result" in new Test {
        val outcome: Right[Nothing, ResponseWrapper[Unit]] = {
          Right(ResponseWrapper(correlationId, ()))
        }

        MockDeleteBenefitAmountsConnector
          .deleteBenefitAmounts(request)
          .returns(Future.successful(outcome))

        val result: ServiceOutcome[Unit] = await(service.delete(request))
        result shouldBe outcome
      }

      "map errors according to spec" when {
        def serviceError(downstreamErrorCode: String, error: MtdError): Unit = {
          s"downstream returns $downstreamErrorCode" in new Test {
            MockDeleteBenefitAmountsConnector
              .deleteBenefitAmounts(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            val result: Either[ErrorWrapper, ResponseWrapper[Unit]] = await(service.delete(request))
            result shouldBe Left(ErrorWrapper(correlationId, error))
          }
        }

        val errors = List(
          "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
          "INVALID_TAX_YEAR"          -> TaxYearFormatError,
          "INVALID_BENEFIT_ID"        -> BenefitIdFormatError,
          "INVALID_CORRELATIONID"     -> InternalError,
          "INVALID_CORRELATION_ID"    -> InternalError,
          "NO_DATA_FOUND"             -> NotFoundError,
          "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
          "SERVER_ERROR"              -> InternalError,
          "SERVICE_UNAVAILABLE"       -> InternalError
        )

        val extraTysErrors = List(
          ("TAX_YEAR_NOT_SUPPORTED", RuleTaxYearNotSupportedError)
        )

        (errors ++ extraTysErrors).foreach(serviceError.tupled)
      }
    }
  }

  private trait Test extends MockDeleteBenefitAmountsConnector {
    val service: DeleteBenefitAmountsService = new DeleteBenefitAmountsService(mockDeleteBenefitAmountsConnector)
  }

}
