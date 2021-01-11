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

package v1.models.domain

import support.UnitSpec
import utils.enums.EnumJsonSpecSupport

class BenefitTypeSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[BenefitType](
    ("otherStateBenefits", BenefitType.otherStateBenefits),
    ("bereavementAllowance", BenefitType.bereavementAllowance),
    ("jobSeekersAllowance", BenefitType.jobSeekersAllowance),
    ("employmentSupportAllowance", BenefitType.employmentSupportAllowance),
    ("statePensionLumpSum", BenefitType.statePensionLumpSum),
    ("statePension", BenefitType.statePension),
  )

  "the Benefit Type" must {
    "return the expected string" in {
      BenefitType.otherStateBenefits.toString shouldBe "otherStateBenefits"
      BenefitType.bereavementAllowance.toString shouldBe "bereavementAllowance"
      BenefitType.jobSeekersAllowance.toString shouldBe "jobSeekersAllowance"
      BenefitType.employmentSupportAllowance.toString shouldBe "employmentSupportAllowance"
      BenefitType.statePensionLumpSum.toString shouldBe "statePensionLumpSum"
      BenefitType.statePension.toString shouldBe "statePension"
    }
  }
}
