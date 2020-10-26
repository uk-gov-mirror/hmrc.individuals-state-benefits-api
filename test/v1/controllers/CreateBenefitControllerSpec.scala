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
import v1.hateoas.HateoasLinks
import v1.mocks.MockCreateBenefitService
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockCreateBenefitRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import v1.models.domain.BenefitType
import v1.models.errors._
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.createBenefit.{CreateBenefitRawData, CreateBenefitRequest, CreateBenefitRequestBody}
import v1.models.response.{AddBenefitHateoasData, AddBenefitResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateBenefitControllerSpec

  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockCreateBenefitService
    with MockAuditService
    with MockCreateBenefitRequestParser
    with MockHateoasFactory
    with HateoasLinks {

  trait Test {
    val hc = HeaderCarrier()

    val controller = new CreateBenefitController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      requestParser = mockAddBenefitRequestParser,
      service = mockCreateStateBenefitService,
      auditService = mockAuditService,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockedAppConfig.apiGatewayContext.returns("individuals/state-benefits").anyNumberOfTimes()

    val links: List[Link] = List(
      listBenefits(mockAppConfig, nino, taxYear, benefitId),
      updateBenefit(mockAppConfig, nino, taxYear, benefitId),
      deleteBenefit(mockAppConfig, nino, taxYear, benefitId)
    )
  }

  val nino: String = "AA123456A"
  val taxYear: String = "2019-20"
  val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val startDate = "2020-08-03"
  val endDate = "2020-12-03"

  val requestBodyJson: JsValue = Json.parse(
    s"""
       |{
       |  "benefitType": "incapacityBenefit",
       |  "startDate": "$startDate",
       |  "endDate" : "$endDate"
       |}
    """.stripMargin
  )

  val rawData: CreateBenefitRawData = CreateBenefitRawData(
    nino = nino,
    taxYear = taxYear,
    body = AnyContentAsJson(requestBodyJson)
  )

  val createStateBenefitRequestBody: CreateBenefitRequestBody = CreateBenefitRequestBody(
    startDate = "2019-01-01",
    endDate = Some("2020-06-01"),
    benefitType = BenefitType.incapacityBenefit.toString
  )

  val requestData: CreateBenefitRequest = CreateBenefitRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    body = createStateBenefitRequestBody
  )

  val responseData: AddBenefitResponse = AddBenefitResponse(benefitId)

  val responseJson: JsValue = Json.parse(
    s"""
       |{
       |   "benefitId": "$benefitId",
       |   "links": [
       |         {
       |         "href": "/individuals/state-benefits/$nino/$taxYear/$benefitId",
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

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "CreateStateBenefit",
      transactionName = "create-state-benefit",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino, "taxYear" -> taxYear),
        request = Some(requestBodyJson),
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  "CreateBenefitController" should {
    "return OK" when {
      "happy path" in new Test {

        MockAddBenefitRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockCreateStateBenefitService
          .createStateBenefit(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseData))))

        MockHateoasFactory
          .wrap(responseData, AddBenefitHateoasData(nino, taxYear, benefitId))
          .returns(HateoasWrapper(responseData, links))

        val result: Future[Result] = controller.createStateBenefit(nino, taxYear)(fakePostRequest(requestBodyJson))

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

            MockAddBenefitRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.createStateBenefit(nino, taxYear)(fakePostRequest(requestBodyJson))

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
          (RuleTaxYearRangeInvalidError, BAD_REQUEST),
          (RuleIncorrectOrEmptyBodyError, BAD_REQUEST),
          (RuleTaxYearNotSupportedError, BAD_REQUEST),
          (RuleTaxYearNotEndedError, BAD_REQUEST),
          (BenefitTypeFormatError, BAD_REQUEST),
          (StartDateFormatError, BAD_REQUEST),
          (EndDateFormatError, BAD_REQUEST),
          (RuleStartDateAfterTaxYearEndError, BAD_REQUEST),
          (RuleEndDateBeforeTaxYearStartError, BAD_REQUEST),
          (RuleEndDateBeforeStartDateError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockAddBenefitRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockCreateStateBenefitService
              .createStateBenefit(requestData)
              .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

            val result: Future[Result] = controller.createStateBenefit(nino, taxYear)(fakePostRequest(requestBodyJson))

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
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}
