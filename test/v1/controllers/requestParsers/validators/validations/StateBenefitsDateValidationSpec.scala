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

package v1.controllers.requestParsers.validators.validations

import support.UnitSpec
import v1.models.errors._

class StateBenefitsDateValidationSpec extends UnitSpec {

  "StateBenefitsDateValidation" when {
    "validate" should {

      "return an empty list when only start date is supplied and it is valid" in {
        StateBenefitsDateValidation.validate(
          startDate = "2020-07-11",
          endDate = None,
          taxYear = "2020-21"
        ) shouldBe NoValidationErrors
      }

      "return an empty list when both dates are supplied and are valid" in {
        StateBenefitsDateValidation.validate(
          startDate = "2020-07-11",
          endDate = Some("2020-07-12"),
          taxYear = "2020-21"
        ) shouldBe NoValidationErrors
      }

      "return multiple format errors when both dates are supplied with the incorrect format" in {
        StateBenefitsDateValidation.validate(
          startDate = "2020-07-111",
          endDate = Some("2020-07-121"),
          taxYear = "2020-21"
        ) shouldBe List(StartDateFormatError, EndDateFormatError)
      }

      "return only a EndDateFormatError when end date format is incorrect and start date is valid" in {
        StateBenefitsDateValidation.validate(
          startDate = "2020-07-11",
          endDate = Some("2020-07-121"),
          taxYear = "2020-21"
        ) shouldBe List(EndDateFormatError)
      }

      "return only a StartDateFormatError when start date format is incorrect and end date is valid" in {
        StateBenefitsDateValidation.validate(
          startDate = "2020-07-111",
          endDate = Some("2020-07-12"),
          taxYear = "2020-21"
        ) shouldBe List(StartDateFormatError)
      }

      "return only a StartDateFormatError when start date format is incorrect and end date is not provided" in {
        StateBenefitsDateValidation.validate(
          startDate = "2020-07-111",
          endDate = None,
          taxYear = "2020-21"
        ) shouldBe List(StartDateFormatError)
      }

      "return multiple errors when end date format is invalid and start date exceeds the tax year" in {
        StateBenefitsDateValidation.validate(
          startDate = "2022-07-11",
          endDate = Some("2022-07-121"),
          taxYear = "2020-21"
        ) shouldBe List(EndDateFormatError, RuleStartDateAfterTaxYearEndError)
      }

      "return multiple errors when start date format is invalid and end date predates the tax year" in {
        StateBenefitsDateValidation.validate(
          startDate = "2020-07-111",
          endDate = Some("2019-07-12"),
          taxYear = "2020-21"
        ) shouldBe List(StartDateFormatError, RuleEndDateBeforeTaxYearStartError)
      }

      // non format error scenarios
      "return RuleStartDateAfterTaxYearEndError when end date is not provided and start date exceeds the tax year" in {
        StateBenefitsDateValidation.validate(
          startDate = "2022-07-11",
          endDate = None,
          taxYear = "2020-21"
        ) shouldBe List(RuleStartDateAfterTaxYearEndError)
      }

      "return multiple errors when start date exceeds both tax year and end date" in {
        StateBenefitsDateValidation.validate(
          startDate = "2022-07-11",
          endDate = Some("2022-07-10"),
          taxYear = "2020-21"
        ) shouldBe List(RuleStartDateAfterTaxYearEndError, RuleEndDateBeforeStartDateError)
      }

      "return multiple errors when end date predates both tax year and start date" in {
        StateBenefitsDateValidation.validate(
          startDate = "2019-07-11",
          endDate = Some("2019-07-10"),
          taxYear = "2020-21"
        ) shouldBe List(RuleEndDateBeforeTaxYearStartError, RuleEndDateBeforeStartDateError)
      }
    }
  }
}
