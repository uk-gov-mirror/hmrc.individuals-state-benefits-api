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
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import support.UnitSpec
import utils.CurrentDateTime
import v1.mocks.MockCurrentDateTime
import v1.models.errors._
import v1.models.request.listBenefits.ListBenefitsRawData

class ListBenefitsValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2020-21"
  private val benefitId = Some("4557ecb5-fd32-48cc-81f5-e6acd1099f3c")


  class Test extends MockCurrentDateTime with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    val validator = new ListBenefitsValidator()

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2022-07-11", dateTimeFormatter))
      .anyNumberOfTimes()

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2021)
  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        validator.validate(ListBenefitsRawData(validNino, validTaxYear, benefitId)) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in new Test {
        validator.validate(ListBenefitsRawData("A12344A", validTaxYear, benefitId)) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in new Test {
        validator.validate(ListBenefitsRawData(validNino, "20199", benefitId)) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "a tax year that is not supported is supplied" in new Test {
        validator.validate(ListBenefitsRawData(validNino, "2019-20", benefitId)) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an out of range tax year is supplied" in new Test {
        validator.validate(ListBenefitsRawData(validNino, "2020-22", benefitId)) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }
    }


    "return BenefitIdFormatError error" when {
      "a an invalid benefit ID is supplied" in new Test {
        validator.validate(ListBenefitsRawData(validNino, "2019-20", Some("invalidBenefitId"))) shouldBe
          List(BenefitIdFormatError)
      }
    }

    "return NinoFormatError, TaxYearFormatError, and BenefitIdFormatError errors" when {
      "request supplied has invalid nino and tax year" in new Test {
        validator.validate(ListBenefitsRawData("A12344A", "20199", Some("invalidBenefitId"))) shouldBe
          List(NinoFormatError, TaxYearFormatError, BenefitIdFormatError)
      }
    }
  }
}