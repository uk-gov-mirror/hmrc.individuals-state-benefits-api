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

package v1.connectors

import mocks.MockAppConfig
import uk.gov.hmrc.domain.Nino
import v1.mocks.MockHttpClient
import v1.models.domain.BenefitType
import v1.models.outcomes.ResponseWrapper
import v1.models.request.createBenefit.{CreateBenefitRequest, CreateBenefitRequestBody}
import v1.models.response.AddBenefitResponse

import scala.concurrent.Future

class CreateBenefitConnectorSpec extends ConnectorSpec {

  val nino: String = "AA111111A"
  val taxYear: String = "2021-22"

  val addBenefitRequestBody: CreateBenefitRequestBody = CreateBenefitRequestBody(
    benefitType = BenefitType.incapacityBenefit.toString,
    startDate = "2020-08-03",
    endDate = Some("2020-12-03")
  )

  val request: CreateBenefitRequest = CreateBenefitRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    body = addBenefitRequestBody
  )

  val response = AddBenefitResponse("b1e8057e-fbbc-47a8-a8b4-78d9f015c253")

  class Test extends MockHttpClient with MockAppConfig {

    val connector: CreateBenefitConnector = new CreateBenefitConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "CreateBenefitConnector" when {
    "createBenefit" should {
      "return a 200 status upon HttpClient success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, response))

        MockedHttpClient
          .post(
            url = s"$baseUrl/income-tax/income/state-benefits/$nino/$taxYear/custom",
            body = addBenefitRequestBody,
            requiredHeaders = "Environment" -> "des-environment", "Authorization" -> s"Bearer des-token"
          ).returns(Future.successful(outcome))

        await(connector.addBenefit(request)) shouldBe outcome
      }
    }
  }
}