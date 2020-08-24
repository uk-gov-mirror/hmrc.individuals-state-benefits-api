/*
 * Copyright 2020 HM Revenue & Customs
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
import v1.mocks.connectors.MockUpdateStateBenefitConnector
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.update.{UpdateStateBenefitsRequest, UpdateStateBenefitsRequestBody}

import scala.concurrent.Future

class UpdateStateBenefitServiceSpec extends ServiceSpec {

  private val nino = "AA123456A"
  private val taxYear = "2021-22"
  private val benefitId = "123e4567-e89b-12d3-a456-426614174000"
  private val correlationId = "X-123"

  val updateStateBenefitsRequestBody: UpdateStateBenefitsRequestBody = UpdateStateBenefitsRequestBody(
    startDate = "2020-08-03",
    endDate = Some("2020-12-03")
  )

  val requestData: UpdateStateBenefitsRequest = UpdateStateBenefitsRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    benefitId = benefitId,
    body = updateStateBenefitsRequestBody
  )

  trait Test extends MockUpdateStateBenefitConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: UpdateStateBenefitService = new UpdateStateBenefitService(
      connector = mockUpdateStateBenefitConnector
    )
  }

  "UpdateStateBenefitService" when {
    "updateStateBenefit" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockUpdateStateBenefitConnector.updateStateBenefit(requestData)
          .returns(Future.successful(outcome))

        await(service.updateStateBenefit(requestData)) shouldBe outcome
      }
    }

    "map errors according to spec" when {

      def serviceError(desErrorCode: String, error: MtdError): Unit =
        s"a $desErrorCode error is returned from the service" in new Test {

          MockUpdateStateBenefitConnector.updateStateBenefit(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

          await(service.updateStateBenefit(requestData)) shouldBe Left(ErrorWrapper(Some(correlationId), error))
        }

      val input = Seq(
        ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
        ("INVALID_TAX_YEAR", TaxYearFormatError),
        ("INVALID_BENEFIT_ID", BenefitIdFormatError),
        ("INVALID_CORRELATIONID", DownstreamError),
        ("INVALID_PAYLOAD", DownstreamError),
        ("UPDATE_FORBIDDEN", RuleUpdateForbiddenError),
        ("NO_DATA_FOUND", NotFoundError),
        ("INVALID_REQUEST_TAX_YEAR", RuleTaxYearNotEndedError),
        ("INVALID_START_DATE", RuleStartDateAfterTaxYearEndError),
        ("INVALID_CESSATION_DATE", RuleEndDateBeforeTaxYearStartError),
        ("SERVER_ERROR", DownstreamError),
        ("SERVICE_UNAVAILABLE", DownstreamError)
      )

      input.foreach(args => (serviceError _).tupled(args))
    }
  }
}
