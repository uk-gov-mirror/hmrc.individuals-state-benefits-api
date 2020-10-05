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
import v1.mocks.requestParsers.MockIgnoreBenefitRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockIgnoreBenefitService, MockMtdIdLookupService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.ignoreBenefit.{IgnoreBenefitRawData, IgnoreBenefitRequest, IgnoreBenefitRequestBody}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IgnoreBenefitControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockIgnoreBenefitService
    with MockIgnoreBenefitRequestParser
    with MockAuditService {

  trait Test {
    val hc = HeaderCarrier()

    val controller = new IgnoreBenefitController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      appConfig = mockAppConfig,
      requestParser = mockIgnoreBenefitRequestParser,
      service = mockIgnoreBenefitService,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockedAppConfig.apiGatewayContext.returns("baseUrl").anyNumberOfTimes()
  }

  val nino: String = "AA123456A"
  val taxYear: String = "2019-20"
  val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val requestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "ignoreBenefit": true
      |}
    """.stripMargin
  )

  val rawData: IgnoreBenefitRawData = IgnoreBenefitRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId,
    body = AnyContentAsJson(requestBodyJson)
  )

  val ignoreBenefitRequestBody: IgnoreBenefitRequestBody = IgnoreBenefitRequestBody(ignoreBenefit = true)

  val requestData: IgnoreBenefitRequest = IgnoreBenefitRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    benefitId = benefitId,
    body = ignoreBenefitRequestBody
  )

  val hateoasResponse: JsValue = Json.parse(
    s"""
      |{
      |   "links":[
      |      {
      |         "href":"/baseUrl/$nino/$taxYear",
      |         "rel":"self",
      |         "method":"GET"
      |      },
      |      {
      |         "href":"/baseUrl/$nino/$taxYear/$benefitId/ignore",
      |         "rel":"ignore-state-benefit",
      |         "method":"PUT"
      |      }
      |   ]
      |}
    """.stripMargin
  )

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "IgnoreStateBenefit",
      transactionName = "ignore-state-benefit",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino, "taxYear" -> taxYear, "benefitId" -> benefitId),
        request = Some(requestBodyJson),
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  "IgnoreBenefitController" should {
    "return OK" when {
      "happy path" in new Test {

        MockIgnoreBenefitRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockIgnoreBenefitService
          .ignoreBenefit(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        val result: Future[Result] = controller.ignoreBenefit(nino, taxYear, benefitId)(fakePutRequest(requestBodyJson))

        status(result) shouldBe OK
        contentAsJson(result) shouldBe hateoasResponse
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(OK, None, Some(hateoasResponse))
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockIgnoreBenefitRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.ignoreBenefit(nino, taxYear, benefitId)(fakePutRequest(requestBodyJson))

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
          (BenefitIdFormatError, BAD_REQUEST),
          (RuleIncorrectOrEmptyBodyError, BAD_REQUEST),
          (RuleTaxYearNotSupportedError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST),
          (RuleTaxYearNotEndedError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockIgnoreBenefitRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockIgnoreBenefitService
              .ignoreBenefit(requestData)
              .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

            val result: Future[Result] = controller.ignoreBenefit(nino, taxYear, benefitId)(fakePutRequest(requestBodyJson))

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
          (RuleTaxYearNotEndedError, BAD_REQUEST),
          (RuleIgnoreForbiddenError, FORBIDDEN),
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}