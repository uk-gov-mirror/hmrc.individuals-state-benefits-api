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
import v1.mocks.connectors.MockAddBenefitConnector
import v1.models.domain.BenefitType
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.addBenefit.{AddBenefitRequest, AddBenefitRequestBody}
import v1.models.response.AddBenefitResponse

import scala.concurrent.Future

class AddBenefitServiceSpec extends ServiceSpec {

  private val nino = "AA112233A"
  private val taxYear = "2021-22"

  val addBenefitRequestBody: AddBenefitRequestBody = AddBenefitRequestBody(
    benefitType = BenefitType.incapacityBenefit.toString,
    startDate = "2020-08-03",
    endDate = Some("2020-12-03")
  )

  val request: AddBenefitRequest = AddBenefitRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    body = addBenefitRequestBody
  )

  val response: AddBenefitResponse = AddBenefitResponse("b1e8057e-fbbc-47a8-a8b4-78d9f015c253")

  trait Test extends MockAddBenefitConnector{
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: AddBenefitService = new AddBenefitService(
      connector = mockAddBenefitConnector
    )
  }

  "AddBenefitService" when {
    "addBenefit" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, response))

        MockAddBenefitConnector.addBenefit(request)
          .returns(Future.successful(outcome))

        await(service.addBenefit(request)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the service" in new Test {

            MockAddBenefitConnector.addBenefit(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

            await(service.addBenefit(request)) shouldBe Left(ErrorWrapper(Some(correlationId), error))
          }

        val input = Seq(
          ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("INVALID_CORRELATIONID", DownstreamError),
          ("INVALID_PAYLOAD", DownstreamError),
          ("INVALID_REQUEST_TAX_YEAR", RuleTaxYearNotSupportedError),
          ("NOT_SUPPORTED_TAX_YEAR", RuleTaxYearNotEndedError),
          ("INVALID_START_DATE", RuleStartDateAfterTaxYearEndError),
          ("INVALID_CESSATION_DATE", RuleEndDateBeforeTaxYearStartError),
          ("SERVER_ERROR", DownstreamError),
          ("SERVICE_UNAVAILABLE", DownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }
  }
}