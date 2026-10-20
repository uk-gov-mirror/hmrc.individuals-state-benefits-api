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
import play.api.libs.ws.WSBodyReadables.readableAsJson
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION

class CreateBenefitControllerISpec extends IntegrationBaseSpec {

  "Calling the 'create state benefit' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.POST, downstreamUri, OK, responseJson)
        }

        val response: WSResponse = await(request().post(requestBodyJson))
        response.status shouldBe OK
        response.body[JsValue] shouldBe responseJson
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {
      val startDate = "2020-08-03"
      val endDate   = "2020-12-03"

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

      val invalidFieldType: MtdError = RuleIncorrectOrEmptyBodyError.copy(
        paths = Some(
          List(
            "/benefitType",
            "/endDate",
            "/startDate"
          ))
      )

      val missingMandatoryFieldErrors: MtdError = RuleIncorrectOrEmptyBodyError.copy(
        paths = Some(
          List(
            "/benefitType",
            "/startDate"
          ))
      )

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError,
                                scenario: Option[String]): Unit = {
          s"validation fails with ${expectedBody.code} error ${scenario.getOrElse("")}" in new Test {

            override val nino: String             = requestNino
            override val taxYear: String          = requestTaxYear
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

        val input = List(
          ("AA1123A", "2019-20", validRequestBodyJson, BAD_REQUEST, NinoFormatError, None),
          ("AA123456A", "20177", validRequestBodyJson, BAD_REQUEST, TaxYearFormatError, None),
          ("AA123456A", "2015-17", validRequestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError, None),
          ("AA123456A", "2015-16", validRequestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError, None),
          ("AA123456A", "2019-20", invalidBenefitIdRequestJson, BAD_REQUEST, BenefitTypeFormatError, None),
          ("AA123456A", "2019-20", invalidStartDateRequestJson, BAD_REQUEST, StartDateFormatError, None),
          ("AA123456A", "2019-20", invalidEndDateRequestJson, BAD_REQUEST, EndDateFormatError, None),
          ("AA123456A", "2019-20", emptyRequestBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None),
          ("AA123456A", "2019-20", nonValidRequestBodyJson, BAD_REQUEST, invalidFieldType, Some("(wrong field type)")),
          ("AA123456A", "2019-20", missingFieldRequestBodyJson, BAD_REQUEST, missingMandatoryFieldErrors, Some("(missing mandatory fields)"))
        )

        input.foreach(validationErrorTest.tupled)
      }

      "ifs service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamStatusCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"ifs returns an $downstreamStatusCode error and status $downstreamStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.POST, downstreamUri, downstreamStatus, errorBody(downstreamStatusCode))
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
             |   "reason": "downstream message"
             |}
            """.stripMargin

        val input = List(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (FORBIDDEN, "NOT_SUPPORTED_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotEndedError),
          (CONFLICT, "BENEFIT_TYPE_ALREADY_EXISTS", BAD_REQUEST, RuleBenefitTypeExists),
          (BAD_REQUEST, "INVALID_START_DATE", BAD_REQUEST, RuleStartDateAfterTaxYearEndError),
          (BAD_REQUEST, "INVALID_CESSATION_DATE", BAD_REQUEST, RuleEndDateBeforeTaxYearStartError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "OUTSIDE_AMENDMENT_WINDOW", BAD_REQUEST, RuleOutsideAmendmentWindow),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
        )

        input.foreach(serviceErrorTest.tupled)
      }
    }
  }

  private trait Test {

    val nino: String              = "AA123456A"
    val taxYear: String           = "2019-20"
    private val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

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

    private def uri: String = s"/$nino/$taxYear"

    def downstreamUri: String = s"/income-tax/income/state-benefits/$nino/$taxYear/custom"

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

}
