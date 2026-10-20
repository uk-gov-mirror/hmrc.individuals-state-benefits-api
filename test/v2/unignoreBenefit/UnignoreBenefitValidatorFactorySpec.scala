/*
 * Copyright 2026 HM Revenue & Customs
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

package v2.unignoreBenefit

import api.controllers.validators.Validator
import api.models.errors.*
import api.utils.UnitSpec
import config.MockStateBenefitsAppConfig
import v2.unignoreBenefit.def1.Def1_UnignoreBenefitValidator
import v2.unignoreBenefit.model.request.UnignoreBenefitRequestData

class UnignoreBenefitValidatorFactorySpec extends UnitSpec with MockStateBenefitsAppConfig {

  private val validNino                      = "AA123456B"
  private val validTaxYear                   = "2021-22"
  private val invalidTaxYear                 = "2021"
  private val validBenefitId                 = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  private implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val validatorFactory = new UnignoreBenefitValidatorFactory

  "validator" should {
    "return the Def1 validator" when {
      "given a valid request" in new AppConfigTest {
        val result: Validator[UnignoreBenefitRequestData] = validatorFactory.validator(validNino, validTaxYear, validBenefitId, true)
        result shouldBe a[Def1_UnignoreBenefitValidator]
      }

      "given an invalid taxYear" in new AppConfigTest {
        val result: Validator[UnignoreBenefitRequestData] = validatorFactory.validator(validNino, invalidTaxYear, validBenefitId, true)
        result shouldBe a[Def1_UnignoreBenefitValidator]
      }
    }

    "return RuleTaxYearNotEndedError error" when {
      "a supplied date range is for a tax year that has not ended and temporal validation is enabled" in new AppConfigTest {
        val result: Either[ErrorWrapper, UnignoreBenefitRequestData] =
          validatorFactory.validator(validNino, "2026-27", validBenefitId, true).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotEndedError))
      }
    }

  }

}
