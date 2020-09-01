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

package v1.controllers

import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.requestParsers.MockUpdateBenefitRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService, MockUpdateBenefitService}
import v1.models.errors.{BadRequestError, BenefitIdFormatError, EndDateFormatError, NinoFormatError, RuleEndDateBeforeStartDateError, RuleEndDateBeforeTaxYearStartError, RuleIncorrectOrEmptyBodyError, RuleStartDateAfterTaxYearEndError, RuleTaxYearNotEndedError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, StartDateFormatError, TaxYearFormatError, _}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.updateBenefit.{UpdateBenefitRawData, UpdateBenefitRequest, UpdateBenefitRequestBody}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateBenefitControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockUpdateBenefitService
    with MockUpdateBenefitRequestParser
    with MockAuditService {

  private val nino: String = "AA123456B"
  private val taxYear: String = "2020-21"
  private val benefitId = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"
  val correlationId: String = "X-123"

  val requestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "startDate": "2020-04-06",
      |   "endDate": "2021-01-01"
      |}
    """.stripMargin
  )

  val rawData: UpdateBenefitRawData = UpdateBenefitRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId,
    body = AnyContentAsJson(requestBodyJson)
  )

  val requestBody: UpdateBenefitRequestBody = UpdateBenefitRequestBody(
    startDate = "2020-04-06",
    endDate = Some("2021-01-01")
  )

  val requestData: UpdateBenefitRequest = UpdateBenefitRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    benefitId = benefitId,
    body = requestBody
  )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new UpdateBenefitController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      appConfig = mockAppConfig,
      requestParser = mockUpdateBenefitRequestParser,
      service = mockUpdateBenefitService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockedAppConfig.apiGatewayContext.returns("individuals/state-benefits").anyNumberOfTimes()
  }

  val responseBody: JsValue = Json.parse(
    s"""
      |{
      |  "links": [
      |    {
      |      "href": "/individuals/state-benefits/$nino/$taxYear/$benefitId",
      |      "method": "PUT",
      |      "rel": "update-state-benefit"
      |    },
      |    {
      |      "href": "/individuals/state-benefits/$nino/$taxYear",
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
      |      "rel": "update-state-benefit-amounts"
      |    }
      |  ]
      |}
    """.stripMargin
  )

  "UpdateBenefitController" should {
    "return OK" when {
      "happy path" in new Test {

        MockUpdateBenefitRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockUpdateBenefitService
          .updateBenefit(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        val result: Future[Result] = controller.update(nino, taxYear, benefitId)(
          fakePutRequest(requestBodyJson)
        )

        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockUpdateBenefitRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.update(nino, taxYear, benefitId)(
              fakePutRequest(requestBodyJson)
            )

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

          }
        }

        val input = Seq(
          (BadRequestError, BAD_REQUEST),
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (RuleTaxYearNotSupportedError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST),
          (RuleIncorrectOrEmptyBodyError, BAD_REQUEST),
          (BenefitIdFormatError, BAD_REQUEST),
          (RuleTaxYearNotEndedError, BAD_REQUEST),
          (StartDateFormatError, BAD_REQUEST),
          (EndDateFormatError, BAD_REQUEST),
          (RuleEndDateBeforeStartDateError, BAD_REQUEST),
          (RuleStartDateAfterTaxYearEndError, BAD_REQUEST),
          (RuleEndDateBeforeTaxYearStartError, BAD_REQUEST),
          (RuleUpdateForbiddenError, FORBIDDEN),
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockUpdateBenefitRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockUpdateBenefitService
              .updateBenefit(requestData)
              .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

            val result: Future[Result] = controller.update(nino, taxYear, benefitId)(
              fakePutRequest(requestBodyJson)
            )

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
          }
        }

        val input = Seq(
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (BenefitIdFormatError, BAD_REQUEST),
          (RuleTaxYearNotEndedError, BAD_REQUEST),
          (RuleStartDateAfterTaxYearEndError, BAD_REQUEST),
          (RuleEndDateBeforeTaxYearStartError, BAD_REQUEST),
          (RuleUpdateForbiddenError, FORBIDDEN),
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR),
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}
