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
import v1.mocks.MockIdGenerator
import v1.mocks.requestParsers.MockAmendBenefitAmountsRequestParser
import v1.mocks.services.{MockAmendBenefitAmountsService, MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.AmendBenefitAmounts.{AmendBenefitAmountsRawData, AmendBenefitAmountsRequest, AmendBenefitAmountsRequestBody}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendBenefitAmountsControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockAmendBenefitAmountsService
    with MockAmendBenefitAmountsRequestParser
    with MockAuditService
    with MockIdGenerator {

  val nino: String = "AA123456A"
  val taxYear: String = "2019-20"
  val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val correlationId: String = "X-123"

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new AmendBenefitAmountsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      appConfig = mockAppConfig,
      requestParser = mockAmendBenefitAmountsRequestParser,
      service = mockUpdateBenefitAmountsService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockedAppConfig.apiGatewayContext.returns("baseUrl").anyNumberOfTimes()
    MockIdGenerator.getCorrelationId.returns(correlationId)
  }

  val requestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "amount": 2050.45,
      |  "taxPaid": 1095.55
      |}
    """.stripMargin
  )

  val rawData: AmendBenefitAmountsRawData = AmendBenefitAmountsRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId,
    body = AnyContentAsJson(requestBodyJson)
  )

  val updateBenefitAmountsRequestBody: AmendBenefitAmountsRequestBody = AmendBenefitAmountsRequestBody(
    amount = 2050.45,
    taxPaid = Some(1095.55)
  )

  val requestData: AmendBenefitAmountsRequest = AmendBenefitAmountsRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    benefitId = benefitId,
    body = updateBenefitAmountsRequestBody
  )

  val hateoasResponse: JsValue = Json.parse(
    s"""
      |{
      |   "links":[
      |      {
      |         "href":"/baseUrl/$nino/$taxYear?benefitId=$benefitId",
      |         "rel":"self",
      |         "method":"GET"
      |      },
      |      {
      |         "href": "/baseUrl/$nino/$taxYear/$benefitId/amounts",
      |         "method": "PUT",
      |         "rel": "amend-state-benefit-amounts"
      |      },
      |      {
      |         "href": "/baseUrl/$nino/$taxYear/$benefitId/amounts",
      |         "method": "DELETE",
      |         "rel": "delete-state-benefit-amounts"
      |      }
      |   ]
      |}
    """.stripMargin
  )

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "AmendStateBenefitAmounts",
      transactionName = "amend-state-benefit-amounts",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino, "taxYear" -> taxYear, "benefitId" -> benefitId),
        request = Some(requestBodyJson),
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  "UpdateBenefitAmountsController" should {
    "return OK" when {
      "happy path" in new Test {

        MockAmendBenefitAmountsRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockUpdateBenefitAmountsService
          .updateBenefitAmounts(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        val result: Future[Result] = controller.amendBenefitAmounts(nino, taxYear, benefitId)(fakePutRequest(requestBodyJson))

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

            MockAmendBenefitAmountsRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(correlationId, error, None)))

            val result: Future[Result] = controller.amendBenefitAmounts(nino, taxYear, benefitId)(fakePutRequest(requestBodyJson))

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
          (ValueFormatError, BAD_REQUEST),
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

            MockAmendBenefitAmountsRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockUpdateBenefitAmountsService
              .updateBenefitAmounts(requestData)
              .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

            val result: Future[Result] = controller.amendBenefitAmounts(nino, taxYear, benefitId)(fakePutRequest(requestBodyJson))

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
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}
