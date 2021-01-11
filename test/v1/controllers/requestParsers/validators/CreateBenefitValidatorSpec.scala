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

import com.typesafe.config.ConfigFactory
import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.Configuration
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import utils.CurrentDateTime
import v1.mocks.MockCurrentDateTime
import v1.models.errors._
import v1.models.request.createBenefit.CreateBenefitRawData

class CreateBenefitValidatorSpec extends UnitSpec {
  private val validNino = "AA123456A"
  private val validTaxYear = "2020-21"

  val startDate = "2020-08-03"
  val endDate = "2020-12-03"

  private val validRequestJson: JsValue = Json.parse(
    s"""
       |{
       |  "benefitType": "statePension",
       |  "startDate": "2019-01-01",
       |  "endDate": "2020-06-01"
       |}
    """.stripMargin
  )

  private val invalidEndDateJson: JsValue = Json.parse(
    s"""
       |{
       |  "benefitType": "statePension",
       |  "startDate": "2019-01-01",
       |  "endDate": "2018-06-01"
       |}
    """.stripMargin
  )

  private val invalidFormatBodyParametersJson: JsValue = Json.parse(
    """
      |{
      |   "benefitType": "InvalidType",
      |   "startDate": "startDate",
      |   "endDate" : "endDate"
      |}
      |""".stripMargin
  )

  private val invalidDateParametersJson: JsValue = Json.parse(
    s"""
       |{
       |  "benefitType": "statePension",
       |  "startDate": "2023-01-01",
       |  "endDate": "2022-06-01"
       |}
    """.stripMargin
  )

  private val invalidParameterTypeJson: JsValue = Json.parse(
    s"""
       |{
       |  "benefitType": true,
       |  "startDate": true,
       |  "endDate": false
       |}
    """.stripMargin
  )

  private val emptyRequestJson: JsValue = JsObject.empty

  private val validRawBody = AnyContentAsJson(validRequestJson)
  private val emptyRawBody = AnyContentAsJson(emptyRequestJson)

  class Test(errorFeatureSwitch: Boolean = true) extends MockCurrentDateTime with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    val validator: CreateBenefitValidator = new CreateBenefitValidator()

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2022-07-11", dateTimeFormatter))
      .anyNumberOfTimes()

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2021)

    MockedAppConfig.featureSwitch.returns(Some(Configuration(ConfigFactory.parseString(
      s"""
         |taxYearNotEndedRule.enabled = $errorFeatureSwitch
      """.stripMargin))))
  }

  "AddBenefitValidator" when {
    "running a validation" should {
      "return no errors for a valid request" in new Test {
        validator.validate(CreateBenefitRawData(validNino, validTaxYear, validRawBody)) shouldBe Nil
      }

      "return RULE_END_DATE_BEFORE_TAX_YEAR_START error when config for Tax RuleTaxYearNotEndedError is set to false" in new Test(false) {
        validator.validate(CreateBenefitRawData(validNino, "2022-23", validRawBody)) shouldBe List(RuleEndDateBeforeTaxYearStartError)
      }

      // parameter format error scenarios
      "return NinoFormatError error when the supplied NINO is invalid" in new Test {
        validator.validate(CreateBenefitRawData("A12344A", validTaxYear, validRawBody)) shouldBe
          List(NinoFormatError)
      }

      "return TaxYearFormatError error for an invalid tax year format" in new Test {
        validator.validate(CreateBenefitRawData(validNino, "20178", validRawBody)) shouldBe
          List(TaxYearFormatError)
      }

      "return RuleTaxYearRangeInvalidError error for an invalid tax year range" in new Test {
        validator.validate(CreateBenefitRawData(validNino, "2018-20", validRawBody)) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }

      "return multiple errors for multiple invalid request parameters" in new Test {
        validator.validate(CreateBenefitRawData("notValid", "2018-20", validRawBody)) shouldBe
          List(NinoFormatError, RuleTaxYearRangeInvalidError)
      }

      // parameter rule error scenarios
      "return RuleTaxYearNotSupportedError error for an unsupported tax year" in new Test {
        validator.validate(CreateBenefitRawData(validNino, "2019-20", validRawBody)) shouldBe
          List(RuleTaxYearNotSupportedError)
      }

      "return RuleTaxYearNotEndedError error for a tax year which hasn't ended" in new Test {
        validator.validate(CreateBenefitRawData(validNino, "2022-23", validRawBody)) shouldBe
          List(RuleTaxYearNotEndedError)
      }

      // body format error scenarios
      "return RuleIncorrectOrEmptyBodyError error for an empty request body" in new Test {
        validator.validate(CreateBenefitRawData(validNino, validTaxYear, emptyRawBody)) shouldBe
          List(RuleIncorrectOrEmptyBodyError)
      }

      "return RuleIncorrectOrEmptyBodyError error for an incorrect request body" in new Test {
        private val paths: Seq[String] = List("/endDate", "/startDate", "/benefitType")

        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(invalidParameterTypeJson))) shouldBe
          List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(paths)))
      }

      "return BenefitTypeFormatError error for an incorrect benefitType" in new Test {
        private val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "invalidBenefit",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(inputJson))) shouldBe
          List(BenefitTypeFormatError)
      }

      "return EndDateBeforeTaxYearStartRuleError error for an incorrect End Date" in new Test {
        private val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "statePension",
             |  "startDate": "2019-12-03",
             |  "endDate" :  "2019-12-03"
             |}
        """.stripMargin
        )

        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(inputJson))) shouldBe
          List(RuleEndDateBeforeTaxYearStartError)
      }

      "return StartDateAfterTaxYearEndRuleError error for an incorrect Start Date" in new Test {
        private val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "statePension",
             |  "startDate": "2022-12-03",
             |  "endDate" :  "2022-12-03"
             |}
        """.stripMargin
        )

        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(inputJson))) shouldBe
          List(RuleStartDateAfterTaxYearEndError)
      }

      "return EndDateBeforeStartDateRuleError error for an incorrect End Date" in new Test {
        private val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "statePension",
             |  "startDate": "$endDate",
             |  "endDate" :  "$startDate"
             |}
        """.stripMargin
        )

        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(inputJson))) shouldBe
          List(RuleEndDateBeforeStartDateError)
      }

      "return EndDateFormatError error for an incorrect End Date" in new Test {
        private val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "statePension",
             |  "startDate": "$startDate",
             |  "endDate" :  "20201203"
             |}
        """.stripMargin
        )

        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(inputJson))) shouldBe
          List(EndDateFormatError)
      }

      "return StartDateFormatError error for an incorrect Start Date" in new Test {
        private val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "statePension",
             |  "startDate": "20200803",
             |  "endDate" :  "$endDate"
             |}
        """.stripMargin
        )

        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(inputJson))) shouldBe
          List(StartDateFormatError)
      }

      // body value error scenarios
      "return multiple errors for incorrect field formats" in new Test {
        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(invalidFormatBodyParametersJson))) shouldBe
          List(BenefitTypeFormatError, StartDateFormatError, EndDateFormatError)
      }

      "return multiple errors for dates which precede the current tax year and are incorrectly ordered" in new Test {
        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(invalidEndDateJson))) shouldBe
          List(RuleEndDateBeforeTaxYearStartError, RuleEndDateBeforeStartDateError)
      }

      "return multiple errors for dates which exceed the current tax year and are incorrectly ordered" in new Test {
        validator.validate(CreateBenefitRawData(validNino, validTaxYear, AnyContentAsJson(invalidDateParametersJson))) shouldBe
          List(RuleStartDateAfterTaxYearEndError, RuleEndDateBeforeStartDateError)
      }
    }
  }

}
