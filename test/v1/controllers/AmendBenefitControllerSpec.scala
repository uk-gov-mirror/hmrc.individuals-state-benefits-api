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
import v1.mocks.requestParsers.MockAmendBenefitRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService, MockAmendBenefitService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import v1.models.errors.{BadRequestError, BenefitIdFormatError, EndDateFormatError, NinoFormatError, RuleEndDateBeforeStartDateError, RuleEndDateBeforeTaxYearStartError, RuleIncorrectOrEmptyBodyError, RuleStartDateAfterTaxYearEndError, RuleTaxYearNotEndedError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, StartDateFormatError, TaxYearFormatError, _}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.AmendBenefit.{AmendBenefitRawData, AmendBenefitRequest, AmendBenefitRequestBody}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendBenefitControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockAmendBenefitService
    with MockAmendBenefitRequestParser
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

  val rawData: AmendBenefitRawData = AmendBenefitRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId,
    body = AnyContentAsJson(requestBodyJson)
  )

  val requestBody: AmendBenefitRequestBody = AmendBenefitRequestBody(
    startDate = "2020-04-06",
    endDate = Some("2021-01-01")
  )

  val requestData: AmendBenefitRequest = AmendBenefitRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    benefitId = benefitId,
    body = requestBody
  )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new AmendBenefitController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      appConfig = mockAppConfig,
      requestParser = mockUpdateBenefitRequestParser,
      service = mockUpdateBenefitService,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockedAppConfig.apiGatewayContext.returns("individuals/state-benefits").anyNumberOfTimes()
  }

  val responseJson: JsValue = Json.parse(
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

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "AmendStateBenefit",
      transactionName = "amend-state-benefit",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino, "taxYear" -> taxYear, "benefitId" -> benefitId),
        request = Some(requestBodyJson),
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
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

        val result: Future[Result] = controller.amend(nino, taxYear, benefitId)(
          fakePutRequest(requestBodyJson)
        )

        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseJson
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(OK, None, Some(responseJson))
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockUpdateBenefitRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.amend(nino, taxYear, benefitId)(
              fakePutRequest(requestBodyJson)
            )

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
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

            val result: Future[Result] = controller.amend(nino, taxYear, benefitId)(
              fakePutRequest(requestBodyJson)
            )

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(mtdError.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
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
