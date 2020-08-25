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
import v1.mocks.validators.MockIgnoreBenefitValidator
import v1.models.errors._
import v1.models.request.ignoreBenefit.{IgnoreBenefitRawData, IgnoreBenefitRequest, IgnoreBenefitRequestBody}

class IgnoreBenefitRequestParserSpec extends UnitSpec {

  private val nino: String = "AA123456B"
  private val taxYear: String = "2021-22"
  private val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val validRequestJson: JsValue = Json.parse(
    """
      |{
      |  "ignoreBenefit": true
      |}
    """.stripMargin
  )

  private val validRawBody = AnyContentAsJson(validRequestJson)

  private val ignoreBenefitRawData = IgnoreBenefitRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId,
    body = validRawBody
  )

  private val ignoreBenefitBodyModel = IgnoreBenefitRequestBody(ignoreBenefit = true)

  private val ignoreBenefitRequest = IgnoreBenefitRequest (
    nino = Nino(nino),
    taxYear = taxYear,
    benefitId = benefitId,
    body = ignoreBenefitBodyModel
  )

  trait Test extends MockIgnoreBenefitValidator {
    lazy val parser: IgnoreBenefitRequestParser = new IgnoreBenefitRequestParser(
      validator = mockIgnoreBenefitValidator
    )
  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockIgnoreBenefitValidator.validate(ignoreBenefitRawData).returns(Nil)
        parser.parseRequest(ignoreBenefitRawData) shouldBe Right(ignoreBenefitRequest)
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        MockIgnoreBenefitValidator.validate(ignoreBenefitRawData.copy(nino = "notANino"))
          .returns(List(NinoFormatError))

        parser.parseRequest(ignoreBenefitRawData.copy(nino = "notANino")) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple path parameter validation errors occur" in new Test {
        MockIgnoreBenefitValidator.validate(ignoreBenefitRawData.copy(nino = "notANino", taxYear = "notATaxYear", benefitId = "notABenefitId"))
          .returns(List(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))

        parser.parseRequest(ignoreBenefitRawData.copy(nino = "notANino", taxYear = "notATaxYear", benefitId = "notABenefitId")) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))))
      }

      "a single field value validation error occur" in new Test {

        private val invalidValueRequestJson: JsValue = Json.parse(
          s"""
             |{
             |  "ignoreBenefit": "notValid"
             |}
            """.stripMargin
        )

        private val invalidValueRawBody = AnyContentAsJson(invalidValueRequestJson)

        private val error = List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/ignoreBenefit"))))

        MockIgnoreBenefitValidator.validate(ignoreBenefitRawData.copy(body = invalidValueRawBody))
          .returns(error)

        parser.parseRequest(ignoreBenefitRawData.copy(body = invalidValueRawBody)) shouldBe
          Left(ErrorWrapper(None, RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/ignoreBenefit"))), None))
      }
    }
  }
}