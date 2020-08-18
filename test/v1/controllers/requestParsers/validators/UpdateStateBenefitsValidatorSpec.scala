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

package v1.controllers.requestParsers.validators

import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import utils.CurrentDateTime
import v1.mocks.MockCurrentDateTime
import v1.models.errors._
import v1.models.request.update.UpdateStateBenefitsRawData

class UpdateStateBenefitsValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2020-21"
  private val benefitId = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

  private val validRequestJson: JsValue = Json.parse(
    """
      |{
      |   "startDate": "2020-04-06",
      |   "endDate": "2021-01-01"
      |}
        """.stripMargin
  )

  private val emptyRequestJson: JsValue = JsObject.empty

  private val noMandatoryFieldsRequestJson: JsValue = Json.parse(
    """
      |{
      | "endDate": "2020-01-01"
      |}
    """.stripMargin
  )

  private val invalidDateTypeRequestJson: JsValue = Json.parse(
    s"""
       |{
       |  "startDate": "notValid",
       |  "endDate": "notValid"
       |}
    """.stripMargin
  )

  private val startDateBeforeEndDateJson: JsValue = Json.parse(
    s"""
       |{
       |  "startDate": "2021-04-06",
       |  "endDate": "2018-01-01"
       |}
    """.stripMargin
  )

  private val notValidTaxYearDates: JsValue = Json.parse(
    s"""
       |{
       |  "startDate": "2022-04-06",
       |  "endDate": "2021-01-01"
       |}
    """.stripMargin
  )

  private val validRawBody = AnyContentAsJson(validRequestJson)
  private val emptyRawBody = AnyContentAsJson(emptyRequestJson)
  private val noMandatoryFieldsRawBody = AnyContentAsJson(noMandatoryFieldsRequestJson)
  private val invalidDateTypeRawBody = AnyContentAsJson(invalidDateTypeRequestJson)
  private val startDateBeforeEndDateRawBody1 = AnyContentAsJson(startDateBeforeEndDateJson)
  private val incorrectDatesRawBody2 = AnyContentAsJson(notValidTaxYearDates)

  //noinspection ScalaStyle
  class Test extends MockCurrentDateTime with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    val validator = new UpdateStateBenefitsValidator()

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2022-07-11", dateTimeFormatter))
      .anyNumberOfTimes()

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2021)
  }

  "UpdateStateBenefitsValidator" when {
    "running a validation" should {
      "return no errors for a valid request" in new Test {
        validator.validate(UpdateStateBenefitsRawData(validNino, validTaxYear, benefitId, validRawBody)) shouldBe Nil
      }

      // parameter format error scenarios
      "return NinoFormatError error when the supplied NINO is invalid" in new Test {
        validator.validate(UpdateStateBenefitsRawData("A12344A", validTaxYear, benefitId, validRawBody)) shouldBe
          List(NinoFormatError)
      }

      "return TaxYearFormatError error for an invalid tax year format" in new Test {
        validator.validate(UpdateStateBenefitsRawData(validNino, "20178", benefitId, validRawBody)) shouldBe
          List(TaxYearFormatError)
      }

      "return RuleTaxYearRangeInvalidError error for an invalid tax year range" in new Test {
        validator.validate(UpdateStateBenefitsRawData(validNino, "2018-20", benefitId, validRawBody)) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }

      "return multiple errors for multiple invalid request parameters" in new Test {
        validator.validate(UpdateStateBenefitsRawData("notValid", "2018-20", benefitId, validRawBody)) shouldBe
          List(NinoFormatError, RuleTaxYearRangeInvalidError)
      }

      // parameter rule error scenarios
      "return RuleTaxYearNotSupportedError error for an unsupported tax year" in new Test {
        validator.validate(UpdateStateBenefitsRawData(validNino, "2018-19", benefitId, validRawBody)) shouldBe
          List(RuleTaxYearNotSupportedError)
      }

      "return RuleTaxYearNotEndedError error for a tax year which hasn't ended" in new Test {
        validator.validate(UpdateStateBenefitsRawData(validNino, "2022-23", benefitId, validRawBody)) shouldBe
          List(RuleTaxYearNotEndedError)
      }

      // body format error scenarios
      "return RuleIncorrectOrEmptyBodyError error for an empty request body" in new Test {
        validator.validate(UpdateStateBenefitsRawData(validNino, validTaxYear, benefitId, emptyRawBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }

      "return RuleIncorrectOrEmptyBodyError error for an incorrect request body" in new Test {
        val paths: Seq[String] = List("/startDate")

        validator.validate(UpdateStateBenefitsRawData(validNino, validTaxYear, benefitId, noMandatoryFieldsRawBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(paths)))
      }

      // body value error scenarios
      "return multiple errors for incorrect field formats" in new Test {
        validator.validate(UpdateStateBenefitsRawData(validNino, validTaxYear, benefitId, invalidDateTypeRawBody)) shouldBe
          List(StartDateFormatError, EndDateFormatError)
      }

      "return multiple errors for dates which precede the current tax year and are incorrectly ordered" in new Test {
        validator.validate(UpdateStateBenefitsRawData(validNino, validTaxYear, benefitId, startDateBeforeEndDateRawBody1)) shouldBe
          List(RuleStartDateAfterTaxYearEndError, RuleEndDateBeforeTaxYearStartError, RuleEndDateBeforeStartDateError)
      }

      "return multiple errors for dates which exceed the current tax year and are incorrectly ordered" in new Test {
        validator.validate(UpdateStateBenefitsRawData(validNino, validTaxYear, benefitId, incorrectDatesRawBody2)) shouldBe
          List(RuleStartDateAfterTaxYearEndError, RuleEndDateBeforeStartDateError)
      }
    }
  }
}
