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

package v1.models.request.addBenefit

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.utils.JsonErrorValidators

class AddBenefitRequestBodySpec extends UnitSpec with JsonErrorValidators {

  val startDate = "2020-08-03"
  val endDate = "2020-12-03"

  "AddBenefitBody" should {
    "read" when {
      "valid benefit with incapacityBenefit" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "incapacityBenefit",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddBenefitRequestBody(incapacityBenefit, startDate, Some(endDate)) shouldBe inputJson.as[AddBenefitRequestBody]
      }

      "valid Benefit with statePension" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "statePension",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddBenefitRequestBody(statePension, startDate, Some(endDate)) shouldBe inputJson.as[AddBenefitRequestBody]
      }

      "valid Benefit with statePensionLumpSum" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "statePensionLumpSum",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddBenefitRequestBody(statePensionLumpSum, startDate, Some(endDate)) shouldBe inputJson.as[AddBenefitRequestBody]
      }

      "valid Benefit with employmentSupportAllowance" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "employmentSupportAllowance",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddBenefitRequestBody(employmentSupportAllowance, startDate, Some(endDate)) shouldBe inputJson.as[AddBenefitRequestBody]
      }

      "valid Benefit with jobSeekersAllowance" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "jobSeekersAllowance",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddBenefitRequestBody(jobSeekersAllowance, startDate, Some(endDate)) shouldBe inputJson.as[AddBenefitRequestBody]
      }

      "valid Benefit with bereavementAllowance" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "bereavementAllowance",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddBenefitRequestBody(bereavementAllowance, startDate, Some(endDate)) shouldBe inputJson.as[AddBenefitRequestBody]
      }

      "valid Benefit with otherStateBenefits" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "otherStateBenefits",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddBenefitRequestBody(otherStateBenefits, startDate, Some(endDate)) shouldBe inputJson.as[AddBenefitRequestBody]
      }

      "a valid body with optional fields missing" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "otherStateBenefits",
             |  "startDate": "$startDate"
             |}
        """.stripMargin
        )

        AddBenefitRequestBody(otherStateBenefits, startDate, None) shouldBe inputJson.as[AddBenefitRequestBody]
      }
    }
    "write" when {
      "a valid full model is provided" in {
        val jsResult = Json.parse(
          s"""
             |{
             |  "benefitType": "otherStateBenefits",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )
        Json.toJson(AddBenefitRequestBody(otherStateBenefits, startDate, Some(endDate))) shouldBe jsResult
      }

      "a valid model with optional fields missing is provided" in {
        val jsResult = Json.parse(
          s"""
             |{
             |  "benefitType": "otherStateBenefits",
             |  "startDate": "$startDate"
             |}
        """.stripMargin
        )
        Json.toJson(AddBenefitRequestBody(otherStateBenefits, startDate, None)) shouldBe jsResult
      }
    }
  }
}
