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

package v2.endpoints

import api.models.errors.*
import api.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import api.support.IntegrationBaseSpec
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status.*
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION

class AmendBenefitControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String    = "AA123456B"
    val taxYear: String = "2019-20"
    val benefitId       = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

    val requestJson: JsValue = Json.parse(
      """
        |{
        |   "startDate": "2019-04-06",
        |   "endDate": "2020-01-01"
        |}
    """.stripMargin
    )

    private def uri: String = s"/$nino/$taxYear/$benefitId"

    def downstreamUri: String = s"/income-tax/income/state-benefits/$nino/$taxYear/custom/$benefitId"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      AuthStub.resetAll()
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

  "Calling the sample endpoint" should {
    "return a 204 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, CREATED)
        }

        val response: WSResponse = await(request().put(requestJson))
        response.status shouldBe NO_CONTENT
      }
    }

    "return a 400 with multiple errors" when {
      "all field value validations fail on the request body" in new Test {

        val json: JsValue = Json.parse(
          """
            |{
            |   "startDate": "2021-04--06",
            |   "endDate": "2020-10--01"
            |}
    """.stripMargin
        )

        private val responseJson = Json.parse("""
            |{
            |	"code": "INVALID_REQUEST",
            |	"message": "Invalid request",
            |	"errors": [
            |     {
            |		"code": "FORMAT_END_DATE",
            |		"message": "The provided End date is invalid"
            |	  },
            |     {
            |		"code": "FORMAT_START_DATE",
            |		"message": "The provided Start date is invalid"
            |	  }
            |   ]
            |}
            |""".stripMargin)

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().put(json))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe responseJson
      }
    }

    "return error according to spec" when {

      val validJson: JsValue = Json.parse(
        """
          |{
          |   "startDate": "2021-04-06",
          |   "endDate": "2022-01-01"
          |}
    """.stripMargin
      )

      val invalidStartDateJson: JsValue = Json.parse(
        """
          |{
          |  "startDate": "nodate",
          |  "endDate": "2020-01-01"
          |}
    """.stripMargin
      )

      val invalidEndDateJson: JsValue = Json.parse(
        """
          |{
          |  "startDate": "2019-04-06",
          |  "endDate": "nodate"
          |}
    """.stripMargin
      )

      val emptyRequestJson: JsValue = JsObject.empty

      val invalidEndBeforeStartJson: JsValue = Json.parse(
        """
          |{
          |   "startDate": "2021-01-01",
          |   "endDate": "2020-04-06"
          |}
    """.stripMargin
      )

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBenefitId: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String         = requestNino
            override val taxYear: String      = requestTaxYear
            override val benefitId: String    = requestBenefitId
            override val requestJson: JsValue = requestBody

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().put(requestJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = List(
          ("AA1123A", "2020-21", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, NinoFormatError),
          ("AA123456A", "20199", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2019-20", "4557ecb-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, BenefitIdFormatError),
          ("AA123456A", "2019-21", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, RuleTaxYearRangeInvalidError),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", emptyRequestJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", invalidStartDateJson, BAD_REQUEST, StartDateFormatError),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", invalidEndDateJson, BAD_REQUEST, EndDateFormatError),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", validJson, BAD_REQUEST, RuleStartDateAfterTaxYearEndError),
          ("AA123456A", "2022-23", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", validJson, BAD_REQUEST, RuleEndDateBeforeTaxYearStartError),
          ("AA123456A", "2018-19", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2020-21", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", invalidEndBeforeStartJson, BAD_REQUEST, RuleEndBeforeStartDateError)
        )

        input.foreach(validationErrorTest.tupled)
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().put(requestJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |  "code": "$code",
             |  "reason": "ifs message"
             |}
            """.stripMargin

        val input = List(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_BENEFIT_ID", BAD_REQUEST, BenefitIdFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (FORBIDDEN, "UPDATE_FORBIDDEN", BAD_REQUEST, RuleUpdateForbiddenError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
          (NOT_FOUND, "NO_DATA_FOUND", NOT_FOUND, NotFoundError),
          (UNPROCESSABLE_ENTITY, "OUTSIDE_AMENDMENT_WINDOW", BAD_REQUEST, RuleOutsideAmendmentWindow),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
        )

        input.foreach(serviceErrorTest.tupled)
      }
    }
  }

}
