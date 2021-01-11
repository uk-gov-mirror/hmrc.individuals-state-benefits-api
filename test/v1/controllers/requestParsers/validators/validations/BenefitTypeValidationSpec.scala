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
import v1.models.errors.BenefitTypeFormatError

class BenefitTypeValidationSpec extends UnitSpec {

  "BenefitTypeValidation" when {
    "validate" must {
      "return an empty list for a valid state pension benefit type" in {
        BenefitTypeValidation.validate(
          benefitType = "statePension"
        ) shouldBe NoValidationErrors
      }

      "return an empty list for a valid state pension lump Sum benefit type" in {

        BenefitTypeValidation.validate(
          benefitType = "statePensionLumpSum"
        ) shouldBe NoValidationErrors
      }

      "return an empty list for a valid employment support Allowance benefit type" in {

        BenefitTypeValidation.validate(
          benefitType = "employmentSupportAllowance"
        ) shouldBe NoValidationErrors
      }

      "return an empty list for a valid job seekers allowance benefit type" in {

        BenefitTypeValidation.validate(
          benefitType = "jobSeekersAllowance"
        ) shouldBe NoValidationErrors
      }

      "return an empty list for a valid state bereavment Allowance type" in {
        BenefitTypeValidation.validate(
          benefitType = "bereavementAllowance"
        ) shouldBe NoValidationErrors
      }

      "return an empty list for a valid other state benefits benefit type" in {
        BenefitTypeValidation.validate(
          benefitType = "otherStateBenefits"
        ) shouldBe NoValidationErrors
      }

      "return a SchemePlanTypeFormatError for a invalid scheme plan type" in {
        BenefitTypeValidation.validate(
          benefitType = "invalid"
        ) shouldBe List(BenefitTypeFormatError)
      }
    }
  }
}
