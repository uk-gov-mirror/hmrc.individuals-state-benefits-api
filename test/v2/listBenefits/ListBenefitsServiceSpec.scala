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

import api.models.domain.{Nino, TaxYear, Timestamp}
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.{ServiceOutcome, ServiceSpec}
import v2.listBenefits.model.request.ListBenefitsRequestData
import v2.listBenefits.model.response.{CustomerStateBenefit, HMRCStateBenefit, ListBenefitsResponse}
import v2.models.domain.BenefitId

import scala.concurrent.Future

class ListBenefitsServiceSpec extends ServiceSpec {

  private val nino      = "AA112233A"
  private val taxYear   = "2019-20"
  private val benefitId = Some("4557ecb5-fd32-48cc-81f5-e6acd1099f3c")

  private val requestData = ListBenefitsRequestData(Nino(nino), TaxYear.fromMtd(taxYear), benefitId.map(BenefitId.apply))

  private val validResponse = ListBenefitsResponse(
    stateBenefits = Some(
      List(
        HMRCStateBenefit(
          benefitType = "incapacityBenefit",
          dateIgnored = Some(Timestamp("2019-04-04T01:01:01.000Z")),
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = None
        )
      )
    ),
    customerAddedStateBenefits = Some(
      List(
        CustomerStateBenefit(
          benefitType = "incapacityBenefit",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = Some(Timestamp("2019-04-04T01:01:01.000Z"))
        )
      )
    )
  )

  "ListBenefitsService" when {
    "listBenefits" must {
      "return correct result for a success" in new Test {
        val outcome: Right[Nothing, ResponseWrapper[ListBenefitsResponse[HMRCStateBenefit, CustomerStateBenefit]]] =
          Right(ResponseWrapper(correlationId, validResponse))

        MockListBenefitsConnector
          .listBenefits(requestData)
          .returns(Future.successful(outcome))

        val result: ServiceOutcome[ListBenefitsResponse[HMRCStateBenefit, CustomerStateBenefit]] = await(service.listBenefits(requestData))
        result shouldBe outcome
      }

      "map errors according to spec" when {
        def serviceError(errorCode: String, error: MtdError): Unit =
          s"a $errorCode error is returned from the service" in new Test {
            MockListBenefitsConnector
              .listBenefits(requestData)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(errorCode))))))

            val result: ServiceOutcome[ListBenefitsResponse[HMRCStateBenefit, CustomerStateBenefit]] = await(service.listBenefits(requestData))
            result shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val errors = List(
          ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("INVALID_BENEFIT_ID", BenefitIdFormatError),
          ("INVALID_VIEW", InternalError),
          ("INVALID_CORRELATIONID", InternalError),
          ("NO_DATA_FOUND", NotFoundError),
          ("TAX_YEAR_NOT_SUPPORTED", RuleTaxYearNotSupportedError),
          ("SERVER_ERROR", InternalError),
          ("SERVICE_UNAVAILABLE", InternalError)
        )

        val extraTysErrors = List(
          ("INVALID_CORRELATION_ID", InternalError),
          ("NOT_FOUND", NotFoundError)
        )

        (errors ++ extraTysErrors).foreach(serviceError.tupled)
      }
    }
  }

  private trait Test extends MockListBenefitsConnector {
    val service: ListBenefitsService = new ListBenefitsService(mockListBenefitsConnector)
  }

}
