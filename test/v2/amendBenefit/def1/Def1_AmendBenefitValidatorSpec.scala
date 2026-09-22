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

package v2.amendBenefit.def1

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.utils.JsonErrorValidators
import api.utils.UnitSpec
import common.errors.BenefitIdFormatError
import config.MockStateBenefitsAppConfig
import play.api.libs.json.{JsObject, JsValue, Json}
import v2.amendBenefit.def1.model.request.{Def1_AmendBenefitRequestBody, Def1_AmendBenefitRequestData}
import v2.amendBenefit.model.request.AmendBenefitRequestData
import v2.models.domain.BenefitId

class Def1_AmendBenefitValidatorSpec extends UnitSpec with JsonErrorValidators with MockStateBenefitsAppConfig {

  private implicit val correlationId: String = "1234"

  private val validNino      = "AA123456A"
  private val validTaxYear   = "2023-24"
  private val validBenefitId = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val startDate    = "2020-04-06"
  private val endDate      = "2021-01-01"
  private val tooEarlyDate = "1809-02-01"
  private val tooLateDate  = "2149-02-21"

  private def validBody(startDate: String = startDate, endDate: String = endDate) = Json.parse(
    s"""
      |{
      |  "startDate": "$startDate",
      |  "endDate": "$endDate"
      |}
      """.stripMargin
  )

  private val parsedNino      = Nino(validNino)
  private val parsedTaxYear   = TaxYear.fromMtd(validTaxYear)
  private val parsedBenefitId = BenefitId(validBenefitId)
  private val parsedBody      = Def1_AmendBenefitRequestBody("2020-04-06", Some("2021-01-01"))

  private def validator(nino: String, taxYear: String, benefitId: String, body: JsValue) =
    new Def1_AmendBenefitValidator(nino, taxYear, benefitId, body)

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody()).validateAndWrapResult()

        result shouldBe Right(Def1_AmendBenefitRequestData(parsedNino, parsedTaxYear, parsedBenefitId, parsedBody))
      }
    }

    "return a single error" when {
      "passed an invalid nino" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator("A12344A", validTaxYear, validBenefitId, validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }

      "passed an invalid tax year" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, "202223", validBenefitId, validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }

      "passed a tax year with an invalid range" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, "2022-24", validBenefitId, validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }

      "passed a tax year that precedes the minimum" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, "2018-19", validBenefitId, validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }

      "passed an invalid benefitId" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] = validator(validNino, validTaxYear, "invalid", validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, BenefitIdFormatError))
      }

      "passed a body where the endDate precedes the start date" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody(endDate, startDate)).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleEndBeforeStartDateError))
      }

      "passed an empty body" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, JsObject.empty).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "passed a body without the required startDate field" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody().removeProperty("/startDate")).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath("/startDate")))
      }

      "passed a body with multiple invalid fields" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody("notValid", "notValid")).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(EndDateFormatError, StartDateFormatError))
          ))
      }

      "passed a body with a start date that precedes the minimum" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody(startDate = tooEarlyDate)).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, StartDateFormatError))
      }

      "passed a body with a start date that precedes the minimum and no endDate" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody(startDate = tooEarlyDate).removeProperty("/endDate")).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, StartDateFormatError))
      }

      "passed a body with a start date that proceeds the maximum" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody(endDate = tooLateDate)).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, EndDateFormatError))
      }

      "passed a start date that is after the tax year end" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody(startDate = tooLateDate)).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleStartDateAfterTaxYearEnd))
      }

      "passed an end date that is before the tax year starts" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody(endDate = tooEarlyDate)).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleEndDateBeforeTaxYearStart))
      }
    }

    "return multiple errors" when {
      "passed multiple invalid fields" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitRequestData] =
          validator("not-a-nino", "not-a-tax-year", "not-a-benefit-id", validBody()).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(BenefitIdFormatError, NinoFormatError, TaxYearFormatError))
          )
        )
      }
    }
  }

}
