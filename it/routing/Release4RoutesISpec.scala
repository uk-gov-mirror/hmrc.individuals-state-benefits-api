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

package routing

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class Release4RoutesISpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] = Map(
    "microservice.services.des.host" -> mockHost,
    "microservice.services.des.port" -> mockPort,
    "microservice.services.mtd-id-lookup.host" -> mockHost,
    "microservice.services.mtd-id-lookup.port" -> mockPort,
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "auditing.consumer.baseUri.port" -> mockPort,
    "minimumPermittedTaxYear" -> 2020,
    "feature-switch.all-endpoints.enabled" -> "false"
  )

  private trait Test {

    val nino: String = "AA123456A"
    val taxYear: String = "2019-20"
    val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

    val requestBodyJson: JsValue = Json.parse(
      """
        |{
        |  "amount": 2050.45,
        |  "taxPaid": 1095.55
        |}
      """.stripMargin
    )

    def uri: String = s"/$nino/$taxYear/$benefitId/amounts"

    def desUri: String = s"/income-tax/income/state-benefits/$nino/$taxYear/$benefitId"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }
  }

  "Calling the 'amend benefit amounts' endpoint (a release 4 endpoint)" should {

    "return a 200 status code" when {
      "the feature switch is turned off to point to release 4 routes only" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
      }
    }
  }

  "Calling the 'amend benefit' endpoint (not a release 4 endpoint)" should {

    "return a 404 status code" when {
      "the feature switch is turned off to point to release 4 routes only" in new Test {

        override def uri: String = s"/$nino/$taxYear/$benefitId"
        override def desUri: String = s"/income-tax/income/state-benefits/$nino/$taxYear/custom/$benefitId"

        override val requestBodyJson: JsValue = Json.parse(
          """
            |{
            |  "startDate": "2019-04-06",
            |  "endDate": "2020-01-01"
            |}
          """.stripMargin
        )

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe NOT_FOUND
      }
    }
  }

}