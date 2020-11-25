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

package v1.models.response

import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v1.models.response.listBenefits.{ListBenefitsResponse, StateBenefit}
import v1.fixtures.ListBenefitsFixture._

class ListBenefitsResponseSpec extends UnitSpec {

  val model: ListBenefitsResponse[StateBenefit] = ListBenefitsResponse(
    stateBenefits = Some(
      Seq(
        StateBenefit(
          benefitType = "incapacityBenefit",
          dateIgnored = Some("2019-04-04T01:01:01Z"),
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = None,
          createdBy = "HMRC"
        ),
        StateBenefit(
          benefitType = "incapacityBenefit",
          dateIgnored = Some("2019-03-04T01:01:01Z"),
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779g",
          startDate = "2020-03-01",
          endDate = Some("2020-04-01"),
          amount = Some(1000.00),
          taxPaid = None,
          submittedOn = None,
          createdBy = "HMRC"
        ),
        StateBenefit(
          benefitType = "statePension",
          dateIgnored = None,
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2019-01-01",
          endDate = None,
          amount = Some(2000.00),
          taxPaid = None,
          submittedOn = None,
          createdBy = "HMRC"
        ),
        StateBenefit(
          benefitType = "statePensionLumpSum",
          dateIgnored = None,
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2019-01-01",
          endDate = Some("2019-01-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = None,
          createdBy = "HMRC"
        ),
        StateBenefit(
          benefitType = "employmentSupportAllowance",
          dateIgnored = None,
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = None,
          createdBy = "HMRC"
        ),
        StateBenefit(
          benefitType = "employmentSupportAllowance",
          dateIgnored = None,
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779g",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(1000.00),
          taxPaid = None,
          submittedOn = None,
          createdBy = "HMRC"
        ),
        StateBenefit(
          benefitType = "jobSeekersAllowance",
          dateIgnored = None,
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = None,
          createdBy = "HMRC"
        ),
        StateBenefit(
          benefitType = "jobSeekersAllowance",
          dateIgnored = None,
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779g",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(1000.00),
          taxPaid = None,
          submittedOn = None,
          createdBy = "HMRC"
        ),
        StateBenefit(
          benefitType = "bereavementAllowance",
          dateIgnored = None,
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = None,
          submittedOn = None,
          createdBy = "HMRC"
        ),
        StateBenefit(
          benefitType = "otherStateBenefits",
          dateIgnored = None,
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = None,
          submittedOn = None,
          createdBy = "HMRC"
        )
      )
    ),
    customerAddedStateBenefits = Some(
      Seq(
        StateBenefit(
          benefitType = "incapacityBenefit",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = Some("2019-04-04T01:01:01Z"),
          createdBy = "CUSTOM"
        ),
        StateBenefit(
          benefitType = "incapacityBenefit",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779g",
          startDate = "2020-03-01",
          endDate = Some("2020-04-01"),
          amount = Some(1000.00),
          taxPaid = None,
          submittedOn = Some("2019-04-04T01:01:01Z"),
          createdBy = "CUSTOM"
        ),
        StateBenefit(
          benefitType = "statePension",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2019-01-01",
          endDate = None,
          amount = Some(2000.00),
          taxPaid = None,
          submittedOn = Some("2019-04-04T01:01:01Z"),
          createdBy = "CUSTOM"
        ),
        StateBenefit(
          benefitType = "statePensionLumpSum",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2019-01-01",
          endDate = Some("2019-01-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = Some("2019-04-04T01:01:01Z"),
          createdBy = "CUSTOM"
        ),
        StateBenefit(
          benefitType = "employmentSupportAllowance",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = Some("2019-04-04T01:01:01Z"),
          createdBy = "CUSTOM"
        ),
        StateBenefit(
          benefitType = "jobSeekersAllowance",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = Some(2132.22),
          submittedOn = Some("2019-04-04T01:01:01Z"),
          createdBy = "CUSTOM"
        ),
        StateBenefit(
          benefitType = "bereavementAllowance",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = None,
          submittedOn = Some("2019-04-04T01:01:01Z"),
          createdBy = "CUSTOM"
        ),
        StateBenefit(
          benefitType = "otherStateBenefits",
          benefitId = "f0d83ac0-a10a-4d57-9e41-6d033832779f",
          startDate = "2020-01-01",
          endDate = Some("2020-04-01"),
          amount = Some(2000.00),
          taxPaid = None,
          submittedOn = Some("2019-04-04T01:01:01Z"),
          createdBy = "CUSTOM"
        )
      )
    )
  )

  val mtdJson: JsValue = Json.parse(
    """
      {
        "stateBenefits": [
          {
            "benefitType": "incapacityBenefit",
            "dateIgnored": "2019-04-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00,
            "taxPaid": 2132.22
          },
          {
            "benefitType": "incapacityBenefit",
            "dateIgnored": "2019-03-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779g",
            "startDate": "2020-03-01",
            "endDate": "2020-04-01",
            "amount": 1000.00
          },
          {
            "benefitType": "statePension",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2019-01-01",
            "amount": 2000.00
          },
          {
            "benefitType": "statePensionLumpSum",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2019-01-01",
            "endDate"  : "2019-01-01",
            "amount": 2000.00,
            "taxPaid": 2132.22
          },
          {
            "benefitType": "employmentSupportAllowance",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00,
            "taxPaid": 2132.22
          },
          {
            "benefitType": "employmentSupportAllowance",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779g",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 1000.00
          },
          {
            "benefitType": "jobSeekersAllowance",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00,
            "taxPaid": 2132.22
          },
          {
            "benefitType": "jobSeekersAllowance",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779g",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 1000.00
          },
          {
            "benefitType": "bereavementAllowance",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00
          },
          {
            "benefitType": "otherStateBenefits",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00
          }
        ],
        "customerAddedStateBenefits": [
          {
            "benefitType": "incapacityBenefit",
            "submittedOn": "2019-04-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00,
            "taxPaid": 2132.22
          },
          {
            "benefitType": "incapacityBenefit",
            "submittedOn": "2019-04-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779g",
            "startDate": "2020-03-01",
            "endDate": "2020-04-01",
            "amount": 1000.00
          },
          {
            "benefitType": "statePension",
            "submittedOn": "2019-04-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2019-01-01",
            "amount": 2000.00
          },
          {
            "benefitType": "statePensionLumpSum",
            "submittedOn": "2019-04-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2019-01-01",
            "endDate" : "2019-01-01",
            "amount": 2000.00,
            "taxPaid": 2132.22
          },
          {
            "benefitType": "employmentSupportAllowance",
            "submittedOn": "2019-04-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00,
            "taxPaid": 2132.22
          },
          {
            "benefitType": "jobSeekersAllowance",
            "submittedOn": "2019-04-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00,
            "taxPaid": 2132.22
          },
          {
            "benefitType": "bereavementAllowance",
            "submittedOn": "2019-04-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00
          },
          {
            "benefitType": "otherStateBenefits",
            "submittedOn": "2019-04-04T01:01:01Z",
            "benefitId": "f0d83ac0-a10a-4d57-9e41-6d033832779f",
            "startDate": "2020-01-01",
            "endDate": "2020-04-01",
            "amount": 2000.00
          }
        ]
      }
      |""".stripMargin)


  "ListBenefitsResponse" when {
    "read from valid JSON" should {

      "produce the expected ListBenefitsResponse object" in {

        desJson.as[ListBenefitsResponse[StateBenefit]] shouldBe model
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {

        Json.toJson(model) shouldBe mtdJson
      }
    }
  }
}
