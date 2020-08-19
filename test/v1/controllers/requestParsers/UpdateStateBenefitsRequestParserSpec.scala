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
import v1.mocks.validators.MockUpdateStateBenefitsValidator
import v1.models.errors._
import v1.models.request.update.{UpdateStateBenefitsRawData, UpdateStateBenefitsRequest, UpdateStateBenefitsRequestBody}

class UpdateStateBenefitsRequestParserSpec extends UnitSpec {

  private val nino: String = "AA123456B"
  private val taxYear: String = "2020-21"
  private val benefitId = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

  private val validRequestJson: JsValue = Json.parse(
    """
      |{
      |  "startDate": "2020-04-06",
      |  "endDate": "2021-01-01"
      |}
    """.stripMargin
  )

  private val validRawBody = AnyContentAsJson(validRequestJson)

  private val updateStateBenefitsRawData = UpdateStateBenefitsRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId,
    body = validRawBody
  )

  private val updateStateBenefitsRequestBody = UpdateStateBenefitsRequestBody(
    startDate = "2020-04-06",
    endDate = Some("2021-01-01")
  )

  private val updateStateBenefitsRequest = UpdateStateBenefitsRequest(
    nino = Nino(nino),
    desTaxYear = taxYear,
    benefitId = benefitId,
    body = updateStateBenefitsRequestBody
  )

  trait Test extends MockUpdateStateBenefitsValidator {
    lazy val parser: UpdateStateBenefitsRequestParser = new UpdateStateBenefitsRequestParser(
      validator = mockUpdateStateBenefitsValidator
    )
  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockUpdateStateBenefitsValidator.validate(updateStateBenefitsRawData).returns(Nil)
        parser.parseRequest(updateStateBenefitsRawData) shouldBe Right(updateStateBenefitsRequest)
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        MockUpdateStateBenefitsValidator.validate(updateStateBenefitsRawData.copy(nino = "notANino"))
          .returns(List(NinoFormatError))

        parser.parseRequest(updateStateBenefitsRawData.copy(nino = "notANino")) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple path parameter validation errors occur" in new Test {
        MockUpdateStateBenefitsValidator.validate(updateStateBenefitsRawData.copy(nino = "notANino", taxYear = "notATaxYear"))
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(updateStateBenefitsRawData.copy(nino = "notANino", taxYear = "notATaxYear")) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }

      "multiple field value validation errors occur" in new Test {

        private val invalidValueRequestJson: JsValue = Json.parse(
          s"""
             |{
             |  "startDate": "noDate",
             |  "endDate": "noDate"
             |}
            """.stripMargin
        )

        private val invalidValueRawBody = AnyContentAsJson(invalidValueRequestJson)

        private val errors = List(
          StartDateFormatError,
          EndDateFormatError
        )

        MockUpdateStateBenefitsValidator.validate(updateStateBenefitsRawData.copy(body = invalidValueRawBody))
          .returns(errors)

        parser.parseRequest(updateStateBenefitsRawData.copy(body = invalidValueRawBody)) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(errors)))
      }
    }
  }
}
