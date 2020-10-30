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

package v1.controllers.requestParsers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v1.mocks.validators.MockAmendBenefitAmountsValidator
import v1.models.errors._
import v1.models.request.AmendBenefitAmounts.{AmendBenefitAmountsRawData, AmendBenefitAmountsRequest, AmendBenefitAmountsRequestBody}

class AmendBenefitAmountsRequestParserSpec extends UnitSpec {

  private val nino: String = "AA123456B"
  private val taxYear: String = "2020-21"
  private val benefitId = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  implicit val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val validRequestJson: JsValue = Json.parse(
    """
      |{
      |  "amount": 2050.45,
      |  "taxPaid": 1095.55
      |}
    """.stripMargin
  )

  private val validRawBody = AnyContentAsJson(validRequestJson)

  private val updateBenefitAmountsRawData = AmendBenefitAmountsRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId,
    body = validRawBody
  )

  private val updateBenefitAmountsRequestBody = AmendBenefitAmountsRequestBody(
    amount = 2050.45,
    taxPaid = Some(1095.55)
  )

  private val updateBenefitAmountsRequest = AmendBenefitAmountsRequest(
    nino = Nino(nino),
    taxYear = taxYear,
    benefitId = benefitId,
    body = updateBenefitAmountsRequestBody
  )

  trait Test extends MockAmendBenefitAmountsValidator {
    lazy val parser: AmendBenefitAmountsRequestParser = new AmendBenefitAmountsRequestParser(
      validator = mockUpdateBenefitAmountsValidator
    )
  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockUpdateBenefitAmountsValidator.validate(updateBenefitAmountsRawData).returns(Nil)
        parser.parseRequest(updateBenefitAmountsRawData) shouldBe Right(updateBenefitAmountsRequest)
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        MockUpdateBenefitAmountsValidator.validate(updateBenefitAmountsRawData.copy(nino = "notANino"))
          .returns(List(NinoFormatError))

        parser.parseRequest(updateBenefitAmountsRawData.copy(nino = "notANino")) shouldBe
          Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }

      "multiple path parameter validation errors occur" in new Test {
        MockUpdateBenefitAmountsValidator.validate(
          updateBenefitAmountsRawData.copy(nino = "notANino", taxYear = "notATaxYear", benefitId = "notABenefitId"))
          .returns(List(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))

        parser.parseRequest(updateBenefitAmountsRawData.copy(nino = "notANino", taxYear = "notATaxYear", benefitId = "notABenefitId")) shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))))
      }

      "multiple field value validation errors occur" in new Test {

        private val allInvalidValueRequestBodyJson: JsValue = Json.parse(
          """
            |{
            |  "amount": -2050.45,
            |  "taxPaid": 1095.558
            |}
          """.stripMargin
        )

        private val allInvalidValueRawRequestBody = AnyContentAsJson(allInvalidValueRequestBodyJson)

        private val allInvalidValueErrors = List(
          ValueFormatError.copy(
            message = "The field should be between 0 and 99999999999.99",
            paths = Some(List("/amount"))
          ),
          ValueFormatError.copy(
            message = "The field should be between -99999999999.99 and 99999999999.99",
            paths = Some(List("/taxPaid"))
          )
        )

        MockUpdateBenefitAmountsValidator.validate(updateBenefitAmountsRawData.copy(body = allInvalidValueRawRequestBody))
          .returns(allInvalidValueErrors)

        parser.parseRequest(updateBenefitAmountsRawData.copy(body = allInvalidValueRawRequestBody)) shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(allInvalidValueErrors)))
      }
    }
  }
}
