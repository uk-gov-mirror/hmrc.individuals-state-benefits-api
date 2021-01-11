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
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class AmendBenefitControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456B"
    val taxYear: String = "2019-20"
    val benefitId = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"
    val correlationId: String = "X-123"

    val requestJson: JsValue = Json.parse(
      """
        |{
        |   "startDate": "2019-04-06",
        |   "endDate": "2020-01-01"
        |}
    """.stripMargin
    )

    def uri: String = s"/$nino/$taxYear/$benefitId"

    def desUri: String = s"/income-tax/income/state-benefits/$nino/$taxYear/custom/$benefitId"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }
  }

  "Calling the sample endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        val mtdResponse: JsValue = Json.parse(
          s"""
             |{
             |  "links": [
             |    {
             |      "href": "/individuals/state-benefits/$nino/$taxYear/$benefitId",
             |      "method": "PUT",
             |      "rel": "amend-state-benefit"
             |    },
             |    {
             |      "href": "/individuals/state-benefits/$nino/$taxYear?benefitId=$benefitId",
             |      "method": "GET",
             |      "rel": "self"
             |    },
             |    {
             |      "href": "/individuals/state-benefits/$nino/$taxYear/$benefitId",
             |      "method": "DELETE",
             |      "rel": "delete-state-benefit"
             |    },
             |    {
             |      "href": "/individuals/state-benefits/$nino/$taxYear/$benefitId/amounts",
             |      "method": "PUT",
             |      "rel": "amend-state-benefit-amounts"
             |    }
             |  ]
             |}
    """.stripMargin
        )

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUri, CREATED)
        }

        val response: WSResponse = await(request().put(requestJson))
        response.status shouldBe OK
        response.json shouldBe mtdResponse
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    def getCurrentTaxYear: String = {
      val currentDate = DateTime.now(DateTimeZone.UTC)

      val taxYearStartDate: DateTime = DateTime.parse(
        currentDate.getYear + "-04-06",
        DateTimeFormat.forPattern("yyyy-MM-dd")
      )

      def fromDesIntToString(taxYear: Int): String =
        (taxYear - 1) + "-" + taxYear.toString.drop(2)

      if (currentDate.isBefore(taxYearStartDate)){
        fromDesIntToString(currentDate.getYear)
      }
      else {
        fromDesIntToString(currentDate.getYear + 1)
      }
    }

    "return a 400 with multiple errors" when {
      "all field value validations fail on the request body" in new Test {

        val json: JsValue = Json.parse(
          """
            |{
            |   "startDate": "2021-04-06",
            |   "endDate": "2020-10-01"
            |}
    """.stripMargin
        )

        private val responseJson = Json.parse(
          """
            |{
            |	"code": "INVALID_REQUEST",
            |	"message": "Invalid request",
            |	"errors": [{
            |		"code": "RULE_START_DATE_AFTER_TAX_YEAR_END",
            |		"message": "The start date cannot be later than the tax year end"
            |	}, {
            |		"code": "RULE_END_DATE_BEFORE_START_DATE",
            |		"message": "The end date cannot be earlier than the start date"
            |	}]
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
          |   "startDate": "2020-04-06",
          |   "endDate": "2021-01-01"
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

      "validation error" when {
        def validationErrorTest(requestNino: String, requestTaxYear: String, requestBenefitId: String,
                                requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String = requestNino
            override val taxYear: String = requestTaxYear
            override val benefitId: String = requestBenefitId
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

        val input = Seq(
          ("AA1123A", "2020-21", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, NinoFormatError),
          ("AA123456A", "20199", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2019-20", "4557ecb-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, BenefitIdFormatError),
          ("AA123456A", "2018-19", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2019-21", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, RuleTaxYearRangeInvalidError),
          ("AA123456A", getCurrentTaxYear, "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validJson, BAD_REQUEST, RuleTaxYearNotEndedError),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", emptyRequestJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", invalidStartDateJson, BAD_REQUEST, StartDateFormatError),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", invalidEndDateJson, BAD_REQUEST, EndDateFormatError)
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
              DesStub.onError(DesStub.PUT, desUri, desStatus, errorBody(desCode))
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
             |  "reason": "des message"
             |}
            """.stripMargin

        val input = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_BENEFIT_ID", BAD_REQUEST, BenefitIdFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, DownstreamError),
          (FORBIDDEN, "UPDATE_FORBIDDEN", FORBIDDEN, RuleUpdateForbiddenError),
          (NOT_FOUND, "NO_DATA_FOUND", NOT_FOUND, NotFoundError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, DownstreamError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, DownstreamError),
          (UNPROCESSABLE_ENTITY, "INVALID_REQUEST_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotEndedError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}
