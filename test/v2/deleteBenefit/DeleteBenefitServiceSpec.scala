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

package v2.deleteBenefit

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.{ServiceOutcome, ServiceSpec}
import v2.deleteBenefit.def1.model.request.Def1_DeleteBenefitRequestData
import v2.models.domain.BenefitId

import scala.concurrent.Future

class DeleteBenefitServiceSpec extends ServiceSpec {

  private val nino      = "AA112233A"
  private val taxYear   = "2019-20"
  private val benefitId = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val request = Def1_DeleteBenefitRequestData(Nino(nino), TaxYear.fromMtd(taxYear), BenefitId(benefitId))

  "DeleteBenefitService" when {
    "a valid request is supplied" must {
      "return correct result for a success" in new Test {
        val outcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        MockDeleteBenefitConnector
          .deleteBenefit(request)
          .returns(Future.successful(outcome))

        val result: ServiceOutcome[Unit] = await(service.deleteBenefit(request))
        result shouldBe outcome
      }

      "map errors according to spec" when {
        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockDeleteBenefitConnector
              .deleteBenefit(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            val result: ServiceOutcome[Unit] = await(service.deleteBenefit(request))
            result shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val errors = List(
          "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
          "INVALID_TAX_YEAR"          -> TaxYearFormatError,
          "INVALID_BENEFIT_ID"        -> BenefitIdFormatError,
          "DELETE_FORBIDDEN"          -> RuleDeleteForbiddenError,
          "INVALID_CORRELATIONID"     -> InternalError,
          "NO_DATA_FOUND"             -> NotFoundError,
          "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
          "SERVER_ERROR"              -> InternalError,
          "SERVICE_UNAVAILABLE"       -> InternalError
        )

        errors.foreach(serviceError.tupled)
      }
    }
  }

  private trait Test extends MockDeleteBenefitConnector {
    val service: DeleteBenefitService = new DeleteBenefitService(mockDeleteBenefitConnector)
  }

}
