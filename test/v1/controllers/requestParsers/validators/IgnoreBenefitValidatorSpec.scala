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

import com.typesafe.config.ConfigFactory
import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.Configuration
import support.UnitSpec
import utils.CurrentDateTime
import v1.mocks.MockCurrentDateTime
import v1.models.errors._
import v1.models.request.ignoreBenefit.IgnoreBenefitRawData

class IgnoreBenefitValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2020-21"
  private val validBenefitId = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  class Test(errorFeatureSwitch: Boolean = true) extends MockCurrentDateTime with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    val validator = new IgnoreBenefitValidator()

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2022-07-11", dateTimeFormatter))
      .anyNumberOfTimes()

    //noinspection ScalaStyle
    MockedAppConfig.minimumPermittedTaxYear
      .returns(2021)

    MockedAppConfig.featureSwitch.returns(Some(Configuration(ConfigFactory.parseString(
      s"""
         |taxYearNotEndedRule.enabled = $errorFeatureSwitch
      """.stripMargin))))
  }

  "IgnoreBenefitValidator" when {
    "running a validation" should {
      "return no errors for a valid request" in new Test {
        validator.validate(IgnoreBenefitRawData(validNino, validTaxYear, validBenefitId)) shouldBe Nil
      }

      "return no errors when config for Tax RuleTaxYearNotEndedError is set to false" in new Test(false) {
        validator.validate(IgnoreBenefitRawData(validNino, "2022-23", validBenefitId)) shouldBe List()
      }

      // parameter format error scenarios
      "return NinoFormatError error when the supplied NINO is invalid" in new Test {
        validator.validate(IgnoreBenefitRawData("A12344A", validTaxYear, validBenefitId)) shouldBe
          List(NinoFormatError)
      }

      "return TaxYearFormatError error for an invalid tax year format" in new Test {
        validator.validate(IgnoreBenefitRawData(validNino, "20199", validBenefitId)) shouldBe
          List(TaxYearFormatError)
      }

      "return RuleTaxYearRangeInvalidError error for an invalid tax year range" in new Test {
        validator.validate(IgnoreBenefitRawData(validNino, "2020-22", validBenefitId)) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }

      "return BenefitIdFormatError error for an invalid benefit ID" in new Test {
        validator.validate(IgnoreBenefitRawData(validNino, validTaxYear, "ABCDE12345FG")) shouldBe
          List(BenefitIdFormatError)
      }

      "return multiple errors for multiple invalid request parameters" in new Test {
        validator.validate(IgnoreBenefitRawData("A12344A", "2020-22", "ABCDE12345FG")) shouldBe
          List(NinoFormatError, RuleTaxYearRangeInvalidError, BenefitIdFormatError)
      }

      // parameter rule error scenarios
      "return RuleTaxYearNotSupportedError error for an unsupported tax year" in new Test {
        validator.validate(IgnoreBenefitRawData(validNino, "2019-20", validBenefitId)) shouldBe
          List(RuleTaxYearNotSupportedError)
      }

      "return RuleTaxYearNotEndedError error for a tax year which hasn't ended" in new Test {
        validator.validate(IgnoreBenefitRawData(validNino, "2022-23", validBenefitId)) shouldBe
          List(RuleTaxYearNotEndedError)
      }
    }
  }
}
