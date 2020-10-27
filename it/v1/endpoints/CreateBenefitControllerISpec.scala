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
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class CreateBenefitControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456A"
    val taxYear: String = "2019-20"
    val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
    val correlationId: String = "X-123"

    val requestBodyJson: JsValue = Json.parse(
      s"""
         |{
         |  "benefitType": "incapacityBenefit",
         |  "startDate": "2019-01-01",
         |  "endDate": "2020-06-01"
         |}
      """.stripMargin
    )

    val responseJson: JsValue = Json.parse(
      s"""
         |{
         |   "benefitId": "$benefitId"
         |}
        """.stripMargin
    )

    val responseWithHateoasLinksJson: JsValue = Json.parse(
      s"""
         |{
         |   "benefitId": "$benefitId",
         |   "links": [
         |         {
         |         "href": "/individuals/state-benefits/$nino/$taxYear?benefitId=$benefitId",
         |         "rel": "self",
         |         "method": "GET"
         |      },
         |      {
         |         "href": "/individuals/state-benefits/$nino/$taxYear/$benefitId",
         |         "rel": "amend-state-benefit",
         |         "method": "PUT"
         |      },
         |      {
         |         "href": "/individuals/state-benefits/$nino/$taxYear/$benefitId",
         |         "rel": "delete-state-benefit",
         |         "method": "DELETE"
         |      }
         |      ]
         |}
    """.stripMargin
    )

    def uri: String = s"/$nino/$taxYear"

    def desUri: String = s"/income-tax/income/state-benefits/$nino/$taxYear/custom"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }
  }

  "Calling the 'create state benefit' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.POST, desUri, OK, responseJson)
        }

        val response: WSResponse = await(request().post(requestBodyJson))
        response.status shouldBe OK
        response.body[JsValue] shouldBe responseWithHateoasLinksJson
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {
      val startDate = "2020-08-03"
      val endDate = "2020-12-03"

      val validRequestBodyJson: JsValue = Json.parse(
        s"""
           |{
           |  "benefitType": "incapacityBenefit",
           |  "startDate": "$startDate",
           |  "endDate" : "$endDate"
           |}
      """.stripMargin
      )

      val emptyRequestBodyJson: JsValue = JsObject.empty

      val nonValidRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "benefitType": true,
          |  "startDate": false,
          |  "endDate": false
          |}
        """.stripMargin
      )

      val missingFieldRequestBodyJson: JsValue = Json.parse(
        s"""
           |{
           |  "endDate": "$endDate"
           |}
        """.stripMargin
      )

      val invalidStartDateRequestJson: JsValue = Json.parse(
        """
          |{
          |  "benefitType": "incapacityBenefit",
          |  "startDate": "notValid",
          |  "endDate": "2020-06-01"
          |}
      """.stripMargin
      )

      val invalidEndDateRequestJson: JsValue = Json.parse(
        """
          |{
          |  "benefitType": "incapacityBenefit",
          |  "startDate": "2019-01-01",
          |  "endDate": "notValid"
          |}
      """.stripMargin
      )

      val invalidBenefitIdRequestJson: JsValue = Json.parse(
        s"""
           |{
           |  "benefitType": "InvalidType",
           |  "startDate": "2019-01-01",
           |  "endDate": "2020-06-01"
           |}
      """.stripMargin
      )

      val invalidDateOrderRequestJson: JsValue = Json.parse(
        """
          |{
          |  "benefitType": "incapacityBenefit",
          |  "startDate": "2020-01-01",
          |  "endDate": "2019-06-01"
          |}
      """.stripMargin
      )

      val startDateLateRequestJson: JsValue = Json.parse(
        """
          |{
          |  "benefitType": "incapacityBenefit",
          |  "startDate": "2023-01-01",
          |  "endDate": "2023-06-01"
          |}
      """.stripMargin
      )

      val EndDateEarlyRequestJson: JsValue = Json.parse(
        """
          |{
          |  "benefitType": "incapacityBenefit",
          |  "startDate": "2018-01-01",
          |  "endDate": "2018-06-01"
          |}
      """.stripMargin
      )

      val invalidFieldType: MtdError = RuleIncorrectOrEmptyBodyError.copy(
        paths = Some(List(
          "/endDate",
          "/startDate",
          "/benefitType"
        ))
      )

      val missingMandatoryFieldErrors: MtdError = RuleIncorrectOrEmptyBodyError.copy(
        paths = Some(List(
          "/startDate",
          "/benefitType"
        ))
      )

      def getCurrentTaxYear: String = {
        val currentDate = DateTime.now(DateTimeZone.UTC)

        val taxYearStartDate: DateTime = DateTime.parse(
          currentDate.getYear + "-04-06",
          DateTimeFormat.forPattern("yyyy-MM-dd")
        )

        def fromDesIntToString(taxYear: Int): String =
          (taxYear - 1) + "-" + taxYear.toString.drop(2)

        if (currentDate.isBefore(taxYearStartDate)) fromDesIntToString(currentDate.getYear)
        else fromDesIntToString(currentDate.getYear + 1)
      }

      "validation error" when {
        def validationErrorTest(requestNino: String, requestTaxYear: String, requestBody: JsValue, expectedStatus: Int,
                                expectedBody: MtdError, scenario: Option[String]): Unit = {
          s"validation fails with ${expectedBody.code} error ${scenario.getOrElse("")}" in new Test {

            override val nino: String = requestNino
            override val taxYear: String = requestTaxYear
            override val requestBodyJson: JsValue = requestBody

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().post(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("AA1123A", "2019-20", validRequestBodyJson, BAD_REQUEST, NinoFormatError, None),
          ("AA123456A", "20177", validRequestBodyJson, BAD_REQUEST, TaxYearFormatError, None),
          ("AA123456A", "2015-17", validRequestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError, None),
          ("AA123456A", "2015-16", validRequestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError, None),
          ("AA123456A", getCurrentTaxYear, validRequestBodyJson, BAD_REQUEST, RuleTaxYearNotEndedError, None),
          ("AA123456A", "2019-20", invalidBenefitIdRequestJson, BAD_REQUEST, BenefitTypeFormatError, None),
          ("AA123456A", "2019-20", invalidStartDateRequestJson, BAD_REQUEST, StartDateFormatError, None),
          ("AA123456A", "2019-20", invalidEndDateRequestJson, BAD_REQUEST, EndDateFormatError, None),
          ("AA123456A", "2019-20", invalidDateOrderRequestJson, BAD_REQUEST, RuleEndDateBeforeStartDateError, None),
          ("AA123456A", "2019-20", startDateLateRequestJson, BAD_REQUEST, RuleStartDateAfterTaxYearEndError, None),
          ("AA123456A", "2019-20", EndDateEarlyRequestJson, BAD_REQUEST, RuleEndDateBeforeTaxYearStartError, None),
          ("AA123456A", "2019-20", emptyRequestBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None),
          ("AA123456A", "2019-20", nonValidRequestBodyJson, BAD_REQUEST, invalidFieldType, Some("(wrong field type)")),
          ("AA123456A", "2019-20", missingFieldRequestBodyJson, BAD_REQUEST, missingMandatoryFieldErrors, Some("(missing mandatory fields)"))
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
              DesStub.onError(DesStub.POST, desUri, desStatus, errorBody(desCode))
            }

            val response: WSResponse = await(request().post(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |   "code": "$code",
             |   "reason": "des message"
             |}
            """.stripMargin

        val input = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (FORBIDDEN, "NOT_SUPPORTED_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotEndedError),
          (BAD_REQUEST, "INVALID_REQUEST_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotSupportedError),
          (BAD_REQUEST, "INVALID_START_DATE", BAD_REQUEST, RuleStartDateAfterTaxYearEndError),
          (BAD_REQUEST,  "INVALID_CESSATION_DATE" , BAD_REQUEST, RuleEndDateBeforeTaxYearStartError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, DownstreamError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, DownstreamError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, DownstreamError))

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}