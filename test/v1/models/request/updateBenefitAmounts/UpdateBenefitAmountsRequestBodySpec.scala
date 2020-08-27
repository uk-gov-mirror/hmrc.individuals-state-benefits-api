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

package v1.models.request.updateBenefitAmounts

import play.api.libs.json._
import support.UnitSpec
import v1.models.utils.JsonErrorValidators

class UpdateBenefitAmountsRequestBodySpec extends UnitSpec with JsonErrorValidators {

  val inputJson: JsValue = Json.parse(
    """
      |{
      |   "amount": 999.99,
      |   "taxPaid": 123.13
      |}
        """.stripMargin
  )

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        inputJson.as[UpdateBenefitAmountsRequestBody] shouldBe UpdateBenefitAmountsRequestBody(amount= 999.99, taxPaid = Some(123.13))
      }

      testMandatoryProperty[UpdateBenefitAmountsRequestBody](inputJson)("/amount")

      testPropertyType[UpdateBenefitAmountsRequestBody](inputJson)(
        path = "/amount",
        replacement = "TEST".toJson,
        expectedError = JsonError.NUMBER_FORMAT_EXCEPTION
      )

      testPropertyType[UpdateBenefitAmountsRequestBody](inputJson)(
        path = "/taxPaid",
        replacement = "NotPaid".toJson,
        expectedError = JsonError.NUMBER_FORMAT_EXCEPTION
      )
    }
  }

  "writes" should {
    "return a json" when {
      "a valid object is supplied" in {
        Json.toJson(UpdateBenefitAmountsRequestBody(amount= 999.99, taxPaid = Some(123.13))) shouldBe inputJson
      }
    }
  }
}
