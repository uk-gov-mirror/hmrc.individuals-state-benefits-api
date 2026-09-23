/*
 * Copyright 2023 HM Revenue & Customs
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

package v2.createBenefit

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.utils.JsonErrorValidators
import api.utils.UnitSpec
import common.errors.{BenefitTypeFormatError, RuleStartDateAfterTaxYearEndError, RuleEndDateBeforeTaxYearStartError}
import config.MockStateBenefitsAppConfig
import play.api.libs.json.*
import v2.createBenefit.def1.model.request.{Def1_CreateBenefitRequestBody, Def1_CreateBenefitRequestData}
import v2.createBenefit.model.request.CreateBenefitRequestData

class CreateBenefitValidatorFactorySpec extends UnitSpec with JsonErrorValidators with MockStateBenefitsAppConfig {
  private implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val validNino    = "AA123456B"
  private val validTaxYear = "2019-20"

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  private val startDate = "2020-08-03"
  private val endDate   = "2020-12-03"

  private val requestBody =
    Json.parse(
      s"""
         |{
         |  "benefitType": "otherStateBenefits",
         |  "startDate": "$startDate",
         |  "endDate": "$endDate"
         |}
      """.stripMargin
    )

  private val parsedCreateBenefitBody = Def1_CreateBenefitRequestBody("otherStateBenefits", startDate, Some(endDate))

  private val validatorFactory = new CreateBenefitValidatorFactory

  private def validator(nino: String, taxYear: String, body: JsValue) =
    validatorFactory.validator(nino, taxYear, body)

  "Validator" should {

    "return the parsed domain object" when {
      "passed a valid request" in new AppConfigTest {
        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, requestBody).validateAndWrapResult()
        result shouldBe Right(Def1_CreateBenefitRequestData(parsedNino, parsedTaxYear, parsedCreateBenefitBody))
      }
    }

    "return a single error" when {
      "passed an invalid nino" in new AppConfigTest {
        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator("A12344A", validTaxYear, requestBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
      "passed an invalid tax year" in new AppConfigTest {
        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, "202223", requestBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }

      "passed a tax year with an invalid range" in new AppConfigTest {
        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, "2022-24", requestBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }

      "passed a tax year that precedes the minimum" in new AppConfigTest {
        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, "2018-19", requestBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }

      "passed a tax year that Has not finished" in new AppConfigTest {
        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, "2026-27", requestBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotEndedError))
      }

      "passed an empty JSON body" in new AppConfigTest {
        val invalidBody: JsObject                                  = JsObject.empty
        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "passed an incorrect request body" in new AppConfigTest {
        val paths: Seq[String] = List("/benefitType", "/endDate", "/startDate")
        val body: JsValue = requestBody
          .update("/benefitType", JsBoolean(true))
          .update("/startDate", JsBoolean(true))
          .update("/endDate", JsBoolean(false))

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPaths(paths)))
      }

      "passed an incorrect benefitType" in new AppConfigTest {
        val body: JsValue = requestBody.update("/benefitType", JsString("invalidBenefit"))

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, BenefitTypeFormatError))
      }

      "passed an incorrect End Date" in new AppConfigTest {
        val body: JsValue = requestBody
          .update("/startDate", JsString(endDate))
          .update("/endDate", JsString(startDate))

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleEndBeforeStartDateError))
      }

      "return EndDateFormatError error for an incorrect End Date" in new AppConfigTest {
        val body: JsValue = requestBody.update("/endDate", JsString("20201203"))

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, EndDateFormatError))
      }

      "passed an incorrect Start Date" in new AppConfigTest {
        val body: JsValue = requestBody.update("/startDate", JsString("20201203"))

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, StartDateFormatError))
      }

      "passed a start date that is before 1900" in new AppConfigTest {
        val body: JsValue = requestBody.update("/startDate", JsString("1899-02-01"))

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, StartDateFormatError))
      }

      "passed a start date that is after 2100 and no end date is provided" in new AppConfigTest {
        val body: JsValue = requestBody
          .update("/startDate", JsString("2101-01-01"))
          .removeProperty("/endDate")

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, StartDateFormatError))
      }

      "passed an end date that is after 2100" in new AppConfigTest {
        val body: JsValue = requestBody.update("/endDate", JsString("2101-01-01"))

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, EndDateFormatError))
      }

      "passed an end date that is before Tax year start date" in new AppConfigTest {
        val body: JsValue =
          Json.parse(
            s"""
               |{
               |  "benefitType": "otherStateBenefits",
               |  "startDate": "2019-01-01",
               |  "endDate": "2019-04-05"
               |}
            """.stripMargin
          )

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleEndDateBeforeTaxYearStartError))
      }

      "passed a start date that is after Tax year end date" in new AppConfigTest {
        val body: JsValue =
          Json.parse(
            s"""
               |{
               |  "benefitType": "otherStateBenefits",
               |  "startDate": "2020-04-06",
               |  "endDate": "2020-06-06"
               |}
            """.stripMargin
          )

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleStartDateAfterTaxYearEndError))
      }
    }

    "return multiple errors" when {
      "passed multiple invalid fields" in new AppConfigTest {
        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator("not-a-nino", "not-a-tax-year", requestBody).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(NinoFormatError, TaxYearFormatError))
          )
        )
      }

      "passed multiple invalid json field formats" in new AppConfigTest {
        val body: JsValue = requestBody
          .update("/benefitType", JsString("invalid"))
          .update("/startDate", JsString("invalid"))
          .update("/endDate", JsString("invalid"))

        val result: Either[ErrorWrapper, CreateBenefitRequestData] = validator(validNino, validTaxYear, body).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(BenefitTypeFormatError, EndDateFormatError, StartDateFormatError))
          )
        )
      }
    }
  }

}
