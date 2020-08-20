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

package v1.models.request.addStateBenefit

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.domain.BenefitType._
import v1.models.utils.JsonErrorValidators

class AddStateBenefitBodySpec extends UnitSpec with JsonErrorValidators {

  val startDate = "2020-08-03"
  val endDate = "2020-12-03"

  "AddStateBenefitBody" should {
    "read" when {
      "valid stateBenfit with incapacityBenefit" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "incapacityBenefit",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddStateBenefitBody(incapacityBenefit, startDate, Some(endDate)) shouldBe inputJson.as[AddStateBenefitBody]
      }

      "valid stateBenefit with statePension" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "statePension",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddStateBenefitBody(statePension, startDate, Some(endDate)) shouldBe inputJson.as[AddStateBenefitBody]
      }

      "valid stateBenfit with statePensionLumpSum" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "statePensionLumpSum",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddStateBenefitBody(statePensionLumpSum, startDate, Some(endDate)) shouldBe inputJson.as[AddStateBenefitBody]
      }

      "valid stateBenfit with employmentSupportAllowance" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "employmentSupportAllowance",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddStateBenefitBody(employmentSupportAllowance, startDate, Some(endDate)) shouldBe inputJson.as[AddStateBenefitBody]
      }

      "valid stateBenfit with jobSeekersAllowance" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "jobSeekersAllowance",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddStateBenefitBody(jobSeekersAllowance, startDate, Some(endDate)) shouldBe inputJson.as[AddStateBenefitBody]
      }

      "valid stateBenfit with bereavementAllowance" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "bereavementAllowance",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddStateBenefitBody(bereavementAllowance, startDate, Some(endDate)) shouldBe inputJson.as[AddStateBenefitBody]
      }

      "valid stateBenfit with otherStateBenefits" in {
        val inputJson = Json.parse(
          s"""
             |{
             |  "benefitType": "otherStateBenefits",
             |  "startDate": "$startDate",
             |  "endDate" : "$endDate"
             |}
        """.stripMargin
        )

        AddStateBenefitBody(otherStateBenefits, startDate, Some(endDate)) shouldBe inputJson.as[AddStateBenefitBody]
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

        AddStateBenefitBody(otherStateBenefits, startDate, None) shouldBe inputJson.as[AddStateBenefitBody]
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
        Json.toJson(AddStateBenefitBody(otherStateBenefits, startDate, Some(endDate))) shouldBe jsResult
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
        Json.toJson(AddStateBenefitBody(otherStateBenefits, startDate, None)) shouldBe jsResult
      }
    }
  }
}
