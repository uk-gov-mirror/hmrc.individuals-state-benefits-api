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
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import v2.fixtures.ListBenefitsFixture.*

class ListBenefitsControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456A"

    def taxYear: String

    def mtdUri: String = s"/$nino/$taxYear"

    def downstreamUri: String

    def setupStubs(): StubMapping

    def request(benefitId: Option[String]): WSRequest = {
      def queryParams: Seq[(String, String)] =
        Seq("benefitId" -> benefitId)
          .collect { case (k, Some(v)) =>
            (k, v)
          }

      AuthStub.resetAll()
      setupStubs()
      buildRequest(mtdUri)
        .addQueryStringParameters(queryParams*)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

  private trait NonTysTest extends Test {
    def taxYear: String = "2020-21"

    def downstreamUri: String = s"/income-tax/income/state-benefits/$nino/$taxYear"
  }

  private trait TysIfsTest extends Test {
    def taxYear: String = "2023-24"

    def downstreamUri: String = s"/income-tax/income/state-benefits/23-24/$nino"

  }

  "Calling the sample endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, OK, ifsJson)
        }

        val response: WSResponse = await(request(None).get())
        response.status shouldBe OK
        response.json shouldBe mtdJson(taxYear)
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "any valid request is made for a TYS year" in new TysIfsTest {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, OK, ifsJson)
        }

        val response: WSResponse = await(request(None).get())
        response.status shouldBe OK
        response.json shouldBe mtdJson(taxYear)
        response.header("Content-Type") shouldBe Some("application/json")

      }
    }

    "return a 200 status code with single state benefit" when {
      "any valid request is made" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, OK, singleStateBenefitDesJson)
        }

        val response: WSResponse = await(request(None).get())
        response.status shouldBe OK
        response.json shouldBe hmrcOnlyResponseBody
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 200 status code with state benefit with duplicate benefitId" when {
      "any valid get request is made" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("benefitId" -> benefitId), OK, singleStateBenefitDesJsonWithDuplicateId)
        }

        val response: WSResponse = await(request(queryBenefitId).get())
        response.status shouldBe OK
        response.json shouldBe duplicateIdResponse
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 200 status code with single state benefit" when {
      "any valid request with benefitId is made" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("benefitId" -> benefitId), OK, singleCustomerStateBenefitDesJson)
        }

        val response: WSResponse = await(request(queryBenefitId).get())
        response.status shouldBe OK
        response.json shouldBe singleRetrieveWithAmountsBenefitId
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 200 status code with single state benefit without amounts" when {
      "a valid request is made" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("benefitId" -> benefitId), OK, ifsJsonWithNoAmounts)
        }

        val response: WSResponse = await(request(queryBenefitId).get())
        response.status shouldBe OK
        response.json shouldBe responseBodyWithNoAmountsBenefitId
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 200 status code" when {
      "a hmrc state benefit is not ignored yet" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("benefitId" -> benefitId), OK, ifsJsonWithNoDateIgnored)
        }

        val response: WSResponse = await(request(queryBenefitId).get())
        response.status shouldBe OK
        response.json shouldBe responseBodyWithoutDateIgnored
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBenefitId: String,
                                expectedStatus: Int,
                                expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new NonTysTest {

            override val nino: String    = requestNino
            override val taxYear: String = requestTaxYear

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request(Some(requestBenefitId)).get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = Seq(
          ("AA1123A", "2020-21", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, NinoFormatError),
          ("AA123456A", "20199", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2020-21", "4557ecb5-fd32-48cc-81f5-e6acd1099", BAD_REQUEST, BenefitIdFormatError),
          ("AA123456A", "2018-19", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2019-21", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", BAD_REQUEST, RuleTaxYearRangeInvalidError)
        )

        input.foreach(validationErrorTest.tupled)
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new NonTysTest {
            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.GET, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request(None).get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |  "code": "$code",
             |  "reason": "error message"
             |}
            """.stripMargin

        val errors = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError),
          (BAD_REQUEST, "INVALID_BENEFIT_ID", BAD_REQUEST, BenefitIdFormatError),
          (BAD_REQUEST, "INVALID_VIEW", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (NOT_FOUND, "NO_DATA_FOUND", NOT_FOUND, NotFoundError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
        )

        val extraTysErrors = Seq(
          (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
          (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError)
        )

        (errors ++ extraTysErrors).foreach(serviceErrorTest.tupled)
      }
    }
  }

}
