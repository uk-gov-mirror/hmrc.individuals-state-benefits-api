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
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class ListBenefitsControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456B"
    val taxYear: String = "2019-20"
    val correlationId: String = "X-123"

    def uri: String = s"/$nino/$taxYear"

    def desUri: String = s"/income-tax/income/state-benefits/$nino/$taxYear"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }
  }

  val json = Json.parse(
    """
      |{
      |  "stateBenefits": {
      |    "incapacityBenefit": [
      |    {
      |      "dateIgnored": "2019-04-04T01:01:01Z",
      |      "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |      "startDate": "2020-01-01",
      |      "endDate": "2020-04-01",
      |      "amount": 2000.00,
      |      "taxPaid": 2132.22
      |     }
      |    ],
      |    "statePension": {
      |      "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |      "startDate": "2019-01-01",
      |      "amount": 2000.00
      |    },
      |    "statePensionLumpSum": {
      |      "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |      "startDate": "2019-01-01",
      |      "endDate"  : "2019-01-01",
      |      "amount": 2000.00,
      |      "taxPaid": 2132.22
      |    },
      |    "employmentSupportAllowance": [
      |      {
      |        "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |        "startDate": "2020-01-01",
      |        "endDate": "2020-04-01",
      |        "amount": 2000.00,
      |        "taxPaid": 2132.22
      |      }
      |    ],
      |    "jobSeekersAllowance": [
      |      {
      |        "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |        "startDate": "2020-01-01",
      |        "endDate": "2020-04-01",
      |        "amount": 2000.00,
      |        "taxPaid": 2132.22
      |      }
      |    ],
      |    "bereavementAllowance": {
      |      "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |      "startDate": "2020-01-01",
      |      "endDate": "2020-04-01",
      |      "amount": 2000.00
      |    },
      |    "otherStateBenefits": {
      |      "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |      "startDate": "2020-01-01",
      |      "endDate": "2020-04-01",
      |      "amount": 2000.00
      |    }
      |  },
      |  "customerAddedStateBenefits": {
      |    "incapacityBenefit": [
      |      {
      |        "submittedOn": "2019-04-04T01:01:01Z",
      |        "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |        "startDate": "2020-01-01",
      |        "endDate": "2020-04-01",
      |        "amount": 2000.00,
      |        "taxPaid": 2132.22
      |      }
      |    ],
      |    "statePension": {
      |      "submittedOn": "2019-04-04T01:01:01Z",
      |      "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |      "startDate": "2019-01-01",
      |      "amount": 2000.00
      |    },
      |    "statePensionLumpSum": {
      |      "submittedOn": "2019-04-04T01:01:01Z",
      |      "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |      "startDate": "2019-01-01",
      |      "endDate" : "2019-01-01",
      |      "amount": 2000.00,
      |      "taxPaid": 2132.22
      |    },
      |    "employmentSupportAllowance": [
      |      {
      |        "submittedOn": "2019-04-04T01:01:01Z",
      |        "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |        "startDate": "2020-01-01",
      |        "endDate": "2020-04-01",
      |        "amount": 2000.00,
      |        "taxPaid": 2132.22
      |      }
      |    ],
      |    "jobSeekersAllowance": [
      |      {
      |        "submittedOn": "2019-04-04T01:01:01Z",
      |        "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |        "startDate": "2020-01-01",
      |        "endDate": "2020-04-01",
      |        "amount": 2000.00,
      |        "taxPaid": 2132.22
      |      }
      |    ],
      |    "bereavementAllowance": {
      |      "submittedOn": "2019-04-04T01:01:01Z",
      |      "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |      "startDate": "2020-01-01",
      |      "endDate": "2020-04-01",
      |      "amount": 2000.00
      |    },
      |    "otherStateBenefits": {
      |      "submittedOn": "2019-04-04T01:01:01Z",
      |      "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |      "startDate": "2020-01-01",
      |      "endDate": "2020-04-01",
      |      "amount": 2000.00
      |    }
      |  }
      |}
      |""".stripMargin
  )

  "Calling the sample endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        val hateoasJson: JsValue = Json.parse(
          s"""
             |{
             |  "links": [
             |      {
             |      "href": "/individuals/state-benefits/$nino/$taxYear",
             |      "method": "POST",
             |      "rel": "add-state-benefit"
             |    },
             |    {
             |      "href": "/individuals/state-benefits/$nino/$taxYear",
             |      "method": "GET",
             |      "rel": "self"
             |    }
             |  ]
             |}
    """.stripMargin
        )

        val mtdResponse: JsValue = (Json.toJson(json).as[JsObject].++(hateoasJson.as[JsObject])).as[JsValue]

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUri, OK, json)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe OK
        response.json shouldBe mtdResponse
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {

      "validation error" when {
        def validationErrorTest(requestNino: String, requestTaxYear: String, requestBenefitId: String,
                                expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String = requestNino
            override val taxYear: String = requestTaxYear

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = Seq(
          ("AA1123A", "2020-21", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, NinoFormatError),
          ("AA123456A", "20199", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2018-19", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2019-21", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, RuleTaxYearRangeInvalidError)
        )

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "des service error" when {
        def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"des returns an $desCode error and status $desStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DesStub.onError(DesStub.GET, desUri, desStatus, errorBody(desCode))
            }

            val response: WSResponse = await(request().get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |  "code": "$code",
             |  "reason": "des message"
             |}
            """.stripMargin

        val input = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_DATE_RANGE", BAD_REQUEST, RuleTaxYearNotSupportedError),
          (BAD_REQUEST, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, DownstreamError),
          (NOT_FOUND, "NO_DATA_FOUND", NOT_FOUND, NotFoundError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, DownstreamError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, DownstreamError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}
