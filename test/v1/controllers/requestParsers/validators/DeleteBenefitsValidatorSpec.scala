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
import support.UnitSpec
import utils.CurrentDateTime
import v1.mocks.MockCurrentDateTime
import v1.models.errors._
import v1.models.request.deleteBenefits.DeleteBenefitsRawData

class DeleteBenefitsValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2020-21"
  private val validBenefitId = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  class Test extends MockCurrentDateTime with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    val validator = new DeleteBenefitsValidator()

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2022-07-11", dateTimeFormatter))
      .anyNumberOfTimes()

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2021)
  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        validator.validate(DeleteBenefitsRawData(validNino, validTaxYear, validBenefitId)) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in new Test {
        validator.validate(DeleteBenefitsRawData("A12344A", validTaxYear, validBenefitId)) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in new Test {
        validator.validate(DeleteBenefitsRawData(validNino, "20178", validBenefitId)) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return BenefitIdFormatError error" when {
      "an invalid benefit ID is supplied" in new Test {
        validator.validate(DeleteBenefitsRawData(validNino, validTaxYear, "ABCDE12345FG")) shouldBe
          List(BenefitIdFormatError)
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "a tax year that is not supported is supplied" in new Test {
        validator.validate(DeleteBenefitsRawData(validNino, "2018-19", validBenefitId)) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an out of range tax year is supplied" in new Test {
        validator.validate(DeleteBenefitsRawData(validNino, "2020-22", validBenefitId)) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }
    }

    "return NinoFormatError and TaxYearFormatError errors" when {
      "request supplied has invalid nino and tax year" in new Test {
        validator.validate(DeleteBenefitsRawData("A12344A", "20199", validBenefitId)) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }
    }

    "return NinoFormatError, TaxYearFormatError and BenefitIdFormatError errors" when {
      "request supplied has invalid nino, tax year and benefit ID" in new Test {
        validator.validate(DeleteBenefitsRawData("A12344A", "20178", "ABCDE12345FG")) shouldBe
          List(NinoFormatError, TaxYearFormatError, BenefitIdFormatError)
      }
    }
  }
}