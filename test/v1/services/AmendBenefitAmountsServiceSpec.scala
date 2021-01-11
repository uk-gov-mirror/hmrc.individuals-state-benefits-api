/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.services

import uk.gov.hmrc.domain.Nino
import v1.controllers.EndpointLogContext
import v1.mocks.connectors.MockAmendBenefitAmountsConnector
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.AmendBenefitAmounts.{AmendBenefitAmountsRequest, AmendBenefitAmountsRequestBody}

import scala.concurrent.Future

class AmendBenefitAmountsServiceSpec extends ServiceSpec {

  private val nino = "AA123456A"
  private val taxYear = "2021-22"
  private val benefitId = "123e4567-e89b-12d3-a456-426614174000"

  val updateBenefitAmountsRequestBody: AmendBenefitAmountsRequestBody = AmendBenefitAmountsRequestBody(
    amount = 999.99,
    taxPaid = Some(123.13)
  )

  val requestData: AmendBenefitAmountsRequest = AmendBenefitAmountsRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    benefitId = benefitId,
    body = updateBenefitAmountsRequestBody
  )

  trait Test extends MockAmendBenefitAmountsConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: AmendBenefitAmountsService = new AmendBenefitAmountsService(
      connector = mockUpdateBenefitAmountsConnector
    )
  }

  "UpdateBenefitAmountsService" when {
    "UpdateBenefitAmounts" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockUpdateBenefitAmountsConnector.updateBenefitAmounts(requestData)
          .returns(Future.successful(outcome))

        await(service.updateBenefitAmounts(requestData)) shouldBe outcome
      }
    }

    "map errors according to spec" when {

      def serviceError(desErrorCode: String, error: MtdError): Unit =
        s"a $desErrorCode error is returned from the service" in new Test {

          MockUpdateBenefitAmountsConnector.updateBenefitAmounts(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

          await(service.updateBenefitAmounts(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val input = Seq(
        ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
        ("INVALID_TAX_YEAR", TaxYearFormatError),
        ("INVALID_BENEFIT_ID", NotFoundError),
        ("INVALID_CORRELATIONID", DownstreamError),
        ("INVALID_PAYLOAD", DownstreamError),
        ("INVALID_REQUEST_BEFORE_TAX_YEAR", RuleTaxYearNotEndedError),
        ("SERVER_ERROR", DownstreamError),
        ("SERVICE_UNAVAILABLE", DownstreamError)
      )

      input.foreach(args => (serviceError _).tupled(args))
    }
  }
}
