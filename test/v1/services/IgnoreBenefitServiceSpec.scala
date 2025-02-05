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
import v1.mocks.connectors.MockIgnoreBenefitConnector
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.ignoreBenefit.IgnoreBenefitRequest

import scala.concurrent.Future

class IgnoreBenefitServiceSpec extends ServiceSpec {

  val nino: String = "AA111111A"
  val taxYear: String = "2019-20"
  val benefitId: String = "123e4567-e89b-12d3-a456-426614174000"

  val request: IgnoreBenefitRequest = IgnoreBenefitRequest(Nino(nino), taxYear, benefitId)

  trait Test extends MockIgnoreBenefitConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: IgnoreBenefitService = new IgnoreBenefitService(
      connector = mockIgnoreBenefitConnector
    )
  }

  "IgnoreBenefitService" when {
    "ignoreBenefit" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockIgnoreBenefitConnector.ignoreBenefit(request)
          .returns(Future.successful(outcome))

        await(service.ignoreBenefit(request)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the service" in new Test {

            MockIgnoreBenefitConnector.ignoreBenefit(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

            await(service.ignoreBenefit(request)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = Seq(
          ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("INVALID_BENEFIT_ID", BenefitIdFormatError),
          ("INVALID_CORRELATIONID", DownstreamError),
          ("IGNORE_FORBIDDEN", RuleIgnoreForbiddenError),
          ("NOT_SUPPORTED_TAX_YEAR", RuleTaxYearNotEndedError),
          ("NO_DATA_FOUND", NotFoundError),
          ("SERVER_ERROR", DownstreamError),
          ("SERVICE_UNAVAILABLE", DownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }
  }
}
