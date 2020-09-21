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
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.hateoas.HateoasLinks
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockListBenefitsRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockListBenefitsService, MockMtdIdLookupService}
import v1.models.errors.{BadRequestError, NinoFormatError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, TaxYearFormatError, _}
import v1.models.hateoas.Method.{GET, POST}
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.listBenefits.{ListBenefitsRawData, ListBenefitsRequest}
import v1.models.response.listBenefits._

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
    with HateoasLinks {

  private val nino: String = "AA123456B"
  private val taxYear: String = "2020-21"
  val correlationId: String = "X-123"

  val rawData: ListBenefitsRawData = ListBenefitsRawData(
    nino = nino,
    taxYear = taxYear
  )

  val requestData: ListBenefitsRequest = ListBenefitsRequest(
    nino = Nino(nino),
    taxYear = taxYear
  )

  val responseData: ListBenefitsResponse[StateBenefit] = ListBenefitsResponse(
    stateBenefits = Some(
      Seq(
        StateBenefit(
          benefitType = "incapacityBenefit",
          dateIgnored = Some("2019-04-04T01:01:01Z"),
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = None
        )
      )
    ),
    customerAddedStateBenefits = Some(
      Seq(
        StateBenefit(
          benefitType = "incapacityBenefit",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = Some("2019-04-04T01:01:01Z")
        )
      )
    )
  )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new ListBenefitsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      appConfig = mockAppConfig,
      requestParser = mockListBenefitsRequestParser,
      service = mockListBenefitsService,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockedAppConfig.apiGatewayContext.returns("individuals/state-benefits").anyNumberOfTimes()

    val links: List[Link] = List(
      listBenefits(mockAppConfig, nino, taxYear),
      addBenefit(mockAppConfig, nino, taxYear)
    )
  }

  val hateosJson: JsValue = Json.parse(
    s"""
       |{
       |  "links": [
       |    {
       |      "href": "/individuals/state-benefits/$nino/$taxYear",
       |      "method": "GET",
       |      "rel": "self"
       |    },
       |    {
       |      "href": "/individuals/state-benefits/$nino/$taxYear",
       |      "method": "POST",
       |      "rel": "add-state-benefit"
       |    }
       |  ]
       |}
    """.stripMargin
  )

  val responseBody: JsValue = Json.parse(
    """
      |{
      |	"stateBenefits": [{
      |		"benefitType": "incapacityBenefit",
      |		"dateIgnored": "2019-04-04T01:01:01Z",
      |		"benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |		"startDate": "2020-01-01",
      |		"endDate": "2020-04-01",
      |		"amount": 2000,
      |		"taxPaid": 2132.22,
      |		"links": [{
      |			"href": "/context/AA123456A/2020-21?benefitId=\"f0d83ac0-a10a-4d57-9e41-6d033832779f\"",
      |			"method": "GET",
      |			"rel": "self"
      |		}]
      |	}],
      |	"customerAddedStateBenefits": [{
      |		"benefitType": "incapacityBenefit",
      |		"submittedOn": "2019-04-04T01:01:01Z",
      |		"benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
      |		"startDate": "2020-01-01",
      |		"endDate": "2020-04-01",
      |		"amount": 2000,
      |		"taxPaid": 2132.22,
      |		"links": [{
      |			"href": "/context/AA123456A/2020-21?benefitId=\"f0d83ac0-a10a-4d57-9e41-6d033832779f\"",
      |			"method": "GET",
      |			"rel": "self"
      |		}]
      |	}],
      |	"links": [{
      |		"href": "/context/AA123456A/2020-21",
      |		"method": "POST",
      |		"rel": "add-state-benefit"
      |	}, {
      |		"href": "/context/AA123456A/2020-21",
      |		"method": "GET",
      |		"rel": "self"
      |	}]
      |}""".stripMargin)

  val stateBenefits: StateBenefit = StateBenefit(
    benefitType = "incapacityBenefit",
    dateIgnored = Some("2019-04-04T01:01:01Z"),
    benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
    startDate = "2020-01-01",
    endDate = Some("2020-04-01"),
    amount = Some(2000.00),
    taxPaid = Some(2132.22),
    submittedOn = None
  )

  val customerAddedStateBenefits: StateBenefit = StateBenefit(
    benefitType = "incapacityBenefit",
    benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
    startDate = "2020-01-01",
    endDate = Some("2020-04-01"),
    amount = Some(2000.00),
    taxPaid = Some(2132.22),
    submittedOn = Some("2019-04-04T01:01:01Z")
  )

  val stateBenefitsLink: Link = Link("/context/AA123456A/2020-21?benefitId=\"f0d83ac0-a10a-4d57-9e41-6d033832779f\"",GET,"self")
  val customerStateBenefitsLink: Link = Link("/context/AA123456A/2020-21?benefitId=\"f0d83ac0-a10a-4d57-9e41-6d033832779f\"",GET,"self")
  val listBenefitsLink: Seq[Link] = List(Link("/context/AA123456A/2020-21",POST,"add-state-benefit"), Link("/context/AA123456A/2020-21",GET,"self"))

  val listBenefitsResponse: ListBenefitsResponse[StateBenefit] = ListBenefitsResponse(
    stateBenefits = Some(Seq(stateBenefits)),
    customerAddedStateBenefits = Some(Seq(customerAddedStateBenefits)
    )
  )

  val hateoasResponse: HateoasWrapper[ListBenefitsResponse[HateoasWrapper[StateBenefit]]] = HateoasWrapper(
    ListBenefitsResponse(
      Some(List(HateoasWrapper(stateBenefits,List(stateBenefitsLink)))),
      Some(List(HateoasWrapper(customerAddedStateBenefits, List(customerStateBenefitsLink))))),
    listBenefitsLink)

  //Json.toJson(responseData).as[JsObject].++(hateosJson.as[JsObject]).as[JsValue]

  "ListBenefitsController" should {
    "return OK" when {
      "happy path" in new Test {

        MockListBenefitsRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockListBenefitsService
          .listBenefits(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseData))))

        MockHateoasFactory
          .wrapList(responseData, ListBenefitsHateoasData(nino, taxYear))
          .returns(hateoasResponse)

        val result: Future[Result] = controller.listBenefits(nino, taxYear)(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseBody
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockListBenefitsRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.listBenefits(nino, taxYear)(fakeGetRequest)

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
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockListBenefitsRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockListBenefitsService
              .listBenefits(requestData)
              .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

            val result: Future[Result] = controller.listBenefits(nino, taxYear)(fakeGetRequest)

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
