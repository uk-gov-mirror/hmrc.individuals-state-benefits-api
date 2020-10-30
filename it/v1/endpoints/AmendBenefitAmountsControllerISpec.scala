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

package v1.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class AmendBenefitAmountsControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456A"
    val taxYear: String = "2019-20"
    val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
    val correlationId: String = "X-123"

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

  "Calling the 'amend benefit amounts' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUri, NO_CONTENT)
        }

        val hateoasResponse: JsValue = Json.parse(
          s"""
             |{
             |   "links":[
             |      {
             |         "href":"/individuals/state-benefits/$nino/$taxYear?benefitId=$benefitId",
             |         "rel":"self",
             |         "method":"GET"
             |      },
             |      {
             |         "href":"/individuals/state-benefits/$nino/$taxYear/$benefitId/amounts",
             |         "rel": "amend-state-benefit-amounts",
             |         "method":"PUT"
             |      },
             |      {
             |         "href":"/individuals/state-benefits/$nino/$taxYear/$benefitId/amounts",
             |         "rel": "delete-state-benefit-amounts",
             |         "method":"DELETE"
             |      }
             |   ]
             |}
           """.stripMargin
        )

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.body[JsValue] shouldBe hateoasResponse
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {

      val validNino: String = "AA123456A"
      val validTaxYear: String = "2019-20"
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

      val invalidFieldTypeErrors: MtdError = RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/taxPaid", "/amount")))

      val missingMandatoryFieldError: MtdError = RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/amount")))

      val allInvalidValueErrors: Seq[MtdError] = Seq(
        ValueFormatError.copy(
          message = "The field should be between 0 and 99999999999.99",
          paths = Some(List("/amount"))
        ),
        ValueFormatError.copy(
          message = "The field should be between -99999999999.99 and 99999999999.99",
          paths = Some(List("/taxPaid"))
        )
      )

      "validation error" when {
        def validationErrorTest(requestNino: String, requestTaxYear: String, requestBenefitId: String, requestBody: JsValue, expectedStatus: Int,
                                expectedBody: ErrorWrapper, scenario: Option[String]): Unit = {
          s"validation fails with ${expectedBody.error} error ${scenario.getOrElse("")}" in new Test {

            override val nino: String = requestNino
            override val taxYear: String = requestTaxYear
            override val benefitId: String = requestBenefitId
            override val requestBodyJson: JsValue = requestBody

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("AA1123A", validTaxYear, validBenefitId, validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", NinoFormatError, None), None),
          (validNino, "20199", validBenefitId, validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", TaxYearFormatError, None), None),
          (validNino, validTaxYear, "ABCDE12345FG", validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", BenefitIdFormatError, None), None),
          (validNino, "2018-19", validBenefitId, validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", RuleTaxYearNotSupportedError, None), None),
          (validNino, "2019-21", validBenefitId, validRequestJson, BAD_REQUEST, ErrorWrapper("X-123", RuleTaxYearRangeInvalidError, None), None),
          (validNino, validTaxYear, validBenefitId, emptyRequestJson, BAD_REQUEST, ErrorWrapper("X-123", RuleIncorrectOrEmptyBodyError, None), None),
          (validNino, validTaxYear, validBenefitId, invalidFieldTypeRequestBody, BAD_REQUEST,
            ErrorWrapper("X-123", invalidFieldTypeErrors, None), Some("(invalid field type)")),
          (validNino, validTaxYear, validBenefitId, missingFieldRequestBodyJson, BAD_REQUEST,
            ErrorWrapper("X-123", missingMandatoryFieldError, None), Some("(missing mandatory field)")),
          (validNino, validTaxYear, validBenefitId, allInvalidValueRequestBodyJson, BAD_REQUEST,
            ErrorWrapper("X-123", BadRequestError, Some(allInvalidValueErrors)), None)
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

            val response: WSResponse = await(request().put(requestBodyJson))
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
          (BAD_REQUEST, "INVALID_BENEFIT_ID", NOT_FOUND, NotFoundError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, DownstreamError),
          (UNPROCESSABLE_ENTITY, "INVALID_REQUEST_BEFORE_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotEndedError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, DownstreamError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, DownstreamError))

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}
