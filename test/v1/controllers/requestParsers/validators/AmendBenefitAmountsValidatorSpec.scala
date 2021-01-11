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

package v1.controllers.requestParsers.validators

import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import utils.CurrentDateTime
import v1.controllers.requestParsers.validators.validations.ValueFormatErrorMessages
import v1.mocks.MockCurrentDateTime
import v1.models.errors._
import v1.models.request.AmendBenefitAmounts.AmendBenefitAmountsRawData

class AmendBenefitAmountsValidatorSpec extends UnitSpec with ValueFormatErrorMessages {

  private val validNino = "AA123456A"
  private val validTaxYear = "2020-21"
  private val validBenefitId = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val validRequestJson: JsValue = Json.parse(
    """
      |{
      |  "amount": 2050.45,
      |  "taxPaid": 1095.55
      |}
        """.stripMargin
  )

  private val emptyRequestJson: JsValue = JsObject.empty

  private val missingMandatoryFieldJson: JsValue = Json.parse(
    """
      |{
      |  "taxPaid": 1095.55
      |}
    """.stripMargin
  )

  private val incorrectFormatRequestJson: JsValue = Json.parse(
    """
      |{
      |  "amount": true,
      |  "taxPaid": []
      |}
    """.stripMargin
  )

  private val allInvalidValueRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "amount": -2050.45,
      |  "taxPaid": 1095.558
      |}
    """.stripMargin
  )

  private val validRawBody = AnyContentAsJson(validRequestJson)
  private val emptyRawBody = AnyContentAsJson(emptyRequestJson)
  private val missingMandatoryFieldRawRequestBody = AnyContentAsJson(missingMandatoryFieldJson)
  private val incorrectFormatRawBody = AnyContentAsJson(incorrectFormatRequestJson)
  private val allInvalidValueRawRequestBody = AnyContentAsJson(allInvalidValueRequestBodyJson)

  class Test extends MockCurrentDateTime with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    val validator = new AmendBenefitAmountsValidator()

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2021)
  }

  "UpdateBenefitAmountsValidator" when {
    "running a validation" should {
      "return no errors for a valid request" in new Test {
        validator.validate(AmendBenefitAmountsRawData(validNino, validTaxYear, validBenefitId, validRawBody)) shouldBe Nil
      }

      // parameter format error scenarios
      "return NinoFormatError error when the supplied NINO is invalid" in new Test {
        validator.validate(AmendBenefitAmountsRawData("A12344A", validTaxYear, validBenefitId, validRawBody)) shouldBe
          List(NinoFormatError)
      }

      "return TaxYearFormatError error for an invalid tax year format" in new Test {
        validator.validate(AmendBenefitAmountsRawData(validNino, "20199", validBenefitId, validRawBody)) shouldBe
          List(TaxYearFormatError)
      }

      "return BenefitIdFormatError error for an invalid benefit ID" in new Test {
        validator.validate(AmendBenefitAmountsRawData(validNino, validTaxYear, "ABCDE12345FG", validRawBody)) shouldBe
          List(BenefitIdFormatError)
      }

      "return RuleTaxYearRangeInvalidError error for an invalid tax year range" in new Test {
        validator.validate(AmendBenefitAmountsRawData(validNino, "2020-22", validBenefitId, validRawBody)) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }

      "return multiple errors for multiple invalid request parameters" in new Test {
        validator.validate(AmendBenefitAmountsRawData("A12344A", "2020-22", "ABCDE12345FG", validRawBody)) shouldBe
          List(NinoFormatError, RuleTaxYearRangeInvalidError, BenefitIdFormatError)
      }

      // parameter rule error scenarios
      "return RuleTaxYearNotSupportedError error for an unsupported tax year" in new Test {
        validator.validate(AmendBenefitAmountsRawData(validNino, "2019-20", validBenefitId, validRawBody)) shouldBe
          List(RuleTaxYearNotSupportedError)
      }

      // body format error scenarios
      "return RuleIncorrectOrEmptyBodyError error for an empty request body" in new Test {
        validator.validate(AmendBenefitAmountsRawData(validNino, validTaxYear, validBenefitId, emptyRawBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }

      "return RuleIncorrectOrEmptyBodyError error for a non-empty JSON body with no mandatory fields provided" in new Test {
        validator.validate(AmendBenefitAmountsRawData(validNino, validTaxYear, validBenefitId, missingMandatoryFieldRawRequestBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/amount"))))
      }

      "return RuleIncorrectOrEmptyBodyError error for an incorrect request body" in new Test {
        validator.validate(AmendBenefitAmountsRawData(validNino, validTaxYear, validBenefitId, incorrectFormatRawBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/taxPaid", "/amount"))))
      }

      // body value error scenarios
      "return ValueFormatError error for incorrect field formats" in new Test {
        validator.validate(AmendBenefitAmountsRawData(validNino, validTaxYear, validBenefitId, allInvalidValueRawRequestBody)) shouldBe
          List(
            ValueFormatError.copy(
              message = ZERO_MINIMUM_INCLUSIVE,
              paths = Some(List("/amount"))
            ),
            ValueFormatError.copy(
              message = BIG_DECIMAL_MINIMUM_INCLUSIVE,
              paths = Some(List("/taxPaid"))
            )
          )
      }
    }
  }
}
