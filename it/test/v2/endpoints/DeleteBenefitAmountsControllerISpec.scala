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
import play.api.libs.json.Json
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION

class DeleteBenefitAmountsControllerISpec extends IntegrationBaseSpec {

  private trait Test {
    def taxYear: String

    def setupStubs(): StubMapping

    def nino: String = "AA123456A"

    def benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

    private def uri: String = s"/$nino/$taxYear/$benefitId/amounts"

    def downstreamUri: String = s"/income-tax/income/state-benefits/$nino/$taxYear/$benefitId"

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

  private trait NonTysTest extends Test {
    override def taxYear: String = "2020-21"
  }

  private trait TysIfsTest extends Test {
    private val tysYear: String = "23-24"

    override def taxYear: String = "2023-24"

    override def downstreamUri: String = s"/income-tax/income/state-benefits/$tysYear/$nino/$benefitId"
  }

  "Calling the 'delete state benefit amounts' endpoint" should {
    "return a 204 status code" when {
      "any valid non-TYS request is made" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.DELETE, downstreamUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().delete())
        response.status shouldBe NO_CONTENT
        response.body shouldBe ""
      }
    }

    "any valid tys request is made" in new TysIfsTest {

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
        DownstreamStub.onSuccess(DownstreamStub.DELETE, downstreamUri, NO_CONTENT)
      }

      val response: WSResponse = await(request().delete())
      response.status shouldBe NO_CONTENT
      response.body shouldBe ""
    }

    "return error according to spec" when {

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBenefitId: String,
                                expectedStatus: Int,
                                expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new NonTysTest {

            override def nino: String = requestNino

            override def taxYear: String = requestTaxYear

            override def benefitId: String = requestBenefitId

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().delete())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = Seq(
          ("AA1123A", "2019-20", "b1e8057e-fbbc-47a8-a8b4-78d9f015c253", BAD_REQUEST, NinoFormatError),
          ("AA123456A", "20199", "78d9f015-a8b4-57b9-8bbc-c253a1e8057e", BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2019-20", "ABCDE12345FG", BAD_REQUEST, BenefitIdFormatError),
          ("AA123456A", "2018-19", "b1e8057e-fbbc-47a8-a8b4-78d9f015c253", BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2019-21", "78d9f015-a8b4-57b9-8bbc-c253a1e8057e", BAD_REQUEST, RuleTaxYearRangeInvalidError)
        )

        input.foreach(validationErrorTest.tupled)
      }

      "ifs service error" when {
        def serviceErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"ifs returns an $ifsCode error and status $ifsStatus" in new NonTysTest {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.DELETE, downstreamUri, ifsStatus, errorBody(ifsCode))
            }

            val response: WSResponse = await(request().delete())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |   "code": "$code",
             |   "reason": "ifs message"
             |}
            """.stripMargin

        val input = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_BENEFIT_ID", BAD_REQUEST, BenefitIdFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "OUTSIDE_AMENDMENT_WINDOW", BAD_REQUEST, RuleOutsideAmendmentWindow),
          (NOT_FOUND, "NO_DATA_FOUND", NOT_FOUND, NotFoundError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
        )

        val extraTysErrors = Seq(
          (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )

        (input ++ extraTysErrors).foreach(serviceErrorTest.tupled)
      }
    }
  }

}
