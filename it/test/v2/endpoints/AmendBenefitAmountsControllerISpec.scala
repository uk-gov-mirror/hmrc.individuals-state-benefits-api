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
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status.*
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION

class AmendBenefitAmountsControllerISpec extends IntegrationBaseSpec {

  "Calling the 'amend benefit amounts' endpoint" should {
    "return a 204 status code" when {
      "any valid request is made" in new NonTysTest {

        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe NO_CONTENT
      }

      "any valid request with a Tax Year Specific (TYS) tax year is made" in new TysTest {

        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe NO_CONTENT
      }
    }

    "return error according to spec" when {

      val validNino: String      = "AA123456A"
      val validTaxYear: String   = "2019-20"
      val validBenefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

      val validRequestJson: JsValue = Json.parse(
        """
          |{
          |  "amount": 2050.45,
          |  "taxPaid": 1095.55
          |}
        """.stripMargin
      )

      val emptyRequestJson: JsValue = JsObject.empty

      val invalidFieldTypeRequestBody: JsValue = Json.parse(
        """
          |{
          |  "amount": true,
          |  "taxPaid": []
          |}
        """.stripMargin
      )

      val missingFieldRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "taxPaid": 1095.55
          |}
        """.stripMargin
      )

      val allInvalidValueRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "amount": -2050.45,
          |  "taxPaid": 1095.558
          |}
        """.stripMargin
      )

      val invalidFieldTypeErrors: MtdError = RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/amount", "/taxPaid")))

      val missingMandatoryFieldError: MtdError = RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/amount")))

      val allInvalidValueErrors: Seq[MtdError] = List(
        ValueFormatError.copy(
          message = "The value must be between 0 and 99999999999.99",
          paths = Some(List("/amount"))
        ),
        ValueFormatError.copy(
          message = "The value must be between -99999999999.99 and 99999999999.99",
          paths = Some(List("/taxPaid"))
        )
      )

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBenefitId: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: ErrorWrapper,
                                scenario: Option[String]): Unit = {
          s"validation fails with ${expectedBody.error} error ${scenario.getOrElse("")}" in new NonTysTest {

            override val nino: String             = requestNino
            override val taxYear: String          = requestTaxYear
            override val benefitId: String        = requestBenefitId
            override val requestBodyJson: JsValue = requestBody

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = List(
          ("AA1123A", validTaxYear, validBenefitId, validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", NinoFormatError, None), None),
          (validNino, "20199", validBenefitId, validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", TaxYearFormatError, None), None),
          (validNino, validTaxYear, "ABCDE12345FG", validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", BenefitIdFormatError, None), None),
          (validNino, "2018-19", validBenefitId, validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", RuleTaxYearNotSupportedError, None), None),
          (validNino, "2019-21", validBenefitId, validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", RuleTaxYearRangeInvalidError, None), None),
          (validNino, validTaxYear, validBenefitId, emptyRequestJson, BAD_REQUEST, ErrorWrapper("X-123", RuleIncorrectOrEmptyBodyError, None), None),
          (
            validNino,
            validTaxYear,
            validBenefitId,
            invalidFieldTypeRequestBody,
            BAD_REQUEST,
            ErrorWrapper("X-123", invalidFieldTypeErrors, None),
            Some("(invalid field type)")),
          (
            validNino,
            validTaxYear,
            validBenefitId,
            missingFieldRequestBodyJson,
            BAD_REQUEST,
            ErrorWrapper("X-123", missingMandatoryFieldError, None),
            Some("(missing mandatory field)")),
          (
            validNino,
            validTaxYear,
            validBenefitId,
            allInvalidValueRequestBodyJson,
            BAD_REQUEST,
            ErrorWrapper("X-123", BadRequestError, Some(allInvalidValueErrors)),
            None)
        )

        input.foreach(validationErrorTest.tupled)
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new NonTysTest {

            override def setupStubs(): Unit = {
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val errors = List(
          (NOT_FOUND, "INCOME_SOURCE_NOT_FOUND", NOT_FOUND, NotFoundError),
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_BENEFIT_ID", BAD_REQUEST, BenefitIdFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "INVALID_REQUEST_BEFORE_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotEndedError),
          (UNPROCESSABLE_ENTITY, "OUTSIDE_AMENDMENT_WINDOW", BAD_REQUEST, RuleOutsideAmendmentWindow),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
        )

        val extraTysErrors = List(
          (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError),
          (UNPROCESSABLE_ENTITY, "TAX_DEDUCTION_NOT_ALLOWED", UNPROCESSABLE_ENTITY, RuleTaxDeductionNotAllowedError)
        )

        (errors ++ extraTysErrors).foreach(serviceErrorTest.tupled)
      }
    }
  }

  private trait Test {

    val nino: String          = "AA123456A"
    val benefitId: String     = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
    val correlationId: String = "X-123"

    def taxYear: String

    val requestBodyJson: JsValue = Json.parse(
      """
        |{
        |  "amount": 2050.45,
        |  "taxPaid": 1095.55
        |}
      """.stripMargin
    )

    private def mtdUri: String = s"/$nino/$taxYear/$benefitId/amounts"

    def downstreamUri: String

    def setupStubs(): Unit = {}

    def request(): WSRequest = {
      AuthStub.resetAll()
      AuditStub.audit()
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      setupStubs()
      buildRequest(mtdUri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

    def errorBody(code: String): String =
      s"""
         |{
         |   "code": "$code",
         |   "reason": "downstream message"
         |}
            """.stripMargin

  }

  private trait NonTysTest extends Test {
    def taxYear: String = "2019-20"

    def downstreamUri: String = s"/income-tax/income/state-benefits/$nino/2019-20/$benefitId"
  }

  private trait TysTest extends Test {
    def taxYear: String = "2023-24"

    def downstreamUri: String = s"/itsa/income-tax/v1/23-24/income/state-benefits/$nino/$benefitId"
  }

}
