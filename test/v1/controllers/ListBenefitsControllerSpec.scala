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
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v1.fixtures.ListBenefitsFixture._
import v1.hateoas.HateoasLinks
import v1.mocks.MockIdGenerator
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockListBenefitsRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockListBenefitsService, MockMtdIdLookupService}
import v1.models.errors.{BadRequestError, NinoFormatError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, TaxYearFormatError, _}
import v1.models.hateoas.Link
import v1.models.outcomes.ResponseWrapper
import v1.models.response.listBenefits.ListBenefitsHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ListBenefitsControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockListBenefitsService
    with MockListBenefitsRequestParser
    with MockHateoasFactory
    with MockAuditService
    with HateoasLinks
    with MockIdGenerator {

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new ListBenefitsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      appConfig = mockAppConfig,
      requestParser = mockListBenefitsRequestParser,
      service = mockListBenefitsService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockedAppConfig.apiGatewayContext.returns("individuals/state-benefits").anyNumberOfTimes()
    MockIdGenerator.getCorrelationId.returns(correlationId)

    val links: List[Link] = List(
      listBenefits(mockAppConfig, nino, taxYear),
      addBenefit(mockAppConfig, nino, taxYear)
    )
  }

  "ListBenefitsController" should {
    "return OK with full HATEOAS" when {
      "happy path" in new Test {

        MockListBenefitsRequestParser
          .parse(rawData(None))
          .returns(Right(requestData(None)))

        MockListBenefitsService
          .listBenefits(requestData(None))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseData))))

        MockHateoasFactory
          .wrapList(responseData, ListBenefitsHateoasData(nino, taxYear, None))
          .returns(hateoasResponse)

        val result: Future[Result] = controller.listBenefits(nino, taxYear, None)(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return OK with no delete amount HATEOAS" when {
      "state benefits has no amount properties" in new Test {

        MockListBenefitsRequestParser
          .parse(rawData(benefitId))
          .returns(Right(requestData(benefitId)))

        MockListBenefitsService
          .listBenefits(requestData(benefitId))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseDataWithNoAmounts))))

        MockHateoasFactory
          .wrapList(responseDataWithNoAmounts, ListBenefitsHateoasData(nino, taxYear, benefitId))
          .returns(hateoasResponseWithOutAmounts)

        val result: Future[Result] = controller.listBenefits(nino, taxYear, benefitId)(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBodyWithNoAmounts
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return OK with only HMRC state benefit HATEOAS" when {
      "only HMRC state benefits returned" in new Test {

        MockListBenefitsRequestParser
          .parse(rawData(None))
          .returns(Right(requestData(None)))

        MockListBenefitsService
          .listBenefits(requestData(None))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseData.copy(customerAddedStateBenefits = None)))))

        MockHateoasFactory
          .wrapList(responseData.copy(customerAddedStateBenefits = None), ListBenefitsHateoasData(nino, taxYear, None))
          .returns(hmrcOnlyHateoasResponse)

        val result: Future[Result] = controller.listBenefits(nino, taxYear, None)(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody.as[JsObject] - "customerAddedStateBenefits"
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return OK with only CUSTOM state benefit HATEOAS" when {
      "only CUSTOM state benefits returned" in new Test {

        MockListBenefitsRequestParser
          .parse(rawData(None))
          .returns(Right(requestData(None)))

        MockListBenefitsService
          .listBenefits(requestData(None))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseData.copy(stateBenefits = None)))))

        MockHateoasFactory
          .wrapList(responseData.copy(stateBenefits = None), ListBenefitsHateoasData(nino, taxYear, None))
          .returns(customOnlyHateoasResponse)

        val result: Future[Result] = controller.listBenefits(nino, taxYear, None)(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody.as[JsObject] - "stateBenefits"
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return OK with single state benefit with HATEOAS" when {
      "benefitId is passed for single retrieval" in new Test {

        MockListBenefitsRequestParser
          .parse(rawData(Some("f0d83ac0-a10a-4d57-9e41-6d033832779g")))
          .returns(Right(requestData(Some("f0d83ac0-a10a-4d57-9e41-6d033832779g"))))

        MockListBenefitsService
          .listBenefits(requestData(Some("f0d83ac0-a10a-4d57-9e41-6d033832779g")))
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseData.copy(stateBenefits = None)))))

        MockHateoasFactory
          .wrapList(responseData.copy(stateBenefits = None), ListBenefitsHateoasData(nino, taxYear, Some("f0d83ac0-a10a-4d57-9e41-6d033832779g")))
          .returns(singleCustomOnlyHateoasResponse)

        val result: Future[Result] = controller.listBenefits(nino, taxYear, Some("f0d83ac0-a10a-4d57-9e41-6d033832779g"))(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe singleRetrieveWithAmounts
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockListBenefitsRequestParser
              .parse(rawData(benefitId))
              .returns(Left(ErrorWrapper(correlationId, error, None)))

            val result: Future[Result] = controller.listBenefits(nino, taxYear, benefitId)(fakeGetRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

          }
        }

        val input = Seq(
          (BadRequestError, BAD_REQUEST),
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (BenefitIdFormatError, BAD_REQUEST),
          (RuleTaxYearNotSupportedError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST),
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockListBenefitsRequestParser
              .parse(rawData(benefitId))
              .returns(Right(requestData(benefitId)))

            MockListBenefitsService
              .listBenefits(requestData(benefitId))
              .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

            val result: Future[Result] = controller.listBenefits(nino, taxYear, benefitId)(fakeGetRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
          }
        }

        val input = Seq(
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR),
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}
