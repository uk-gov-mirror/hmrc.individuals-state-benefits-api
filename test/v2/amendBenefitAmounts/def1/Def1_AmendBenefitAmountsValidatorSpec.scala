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

package v2.amendBenefitAmounts.def1

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.models.utils.JsonErrorValidators
import api.utils.UnitSpec
import config.MockStateBenefitsAppConfig
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import v2.amendBenefitAmounts.def1.model.request.{Def1_AmendBenefitAmountsRequestBody, Def1_AmendBenefitAmountsRequestData}
import v2.amendBenefitAmounts.model.request.AmendBenefitAmountsRequestData
import v2.models.domain.BenefitId

class Def1_AmendBenefitAmountsValidatorSpec extends UnitSpec with JsonErrorValidators with MockStateBenefitsAppConfig {

  private implicit val correlationId: String = "1234"

  private val validNino      = "AA123456A"
  private val validTaxYear   = "2023-24"
  private val validBenefitId = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val validBody = Json.parse("""
      |{
      |  "amount": 2050.45,
      |  "taxPaid": 1095.55
      |}""".stripMargin)

  private val parsedNino      = Nino(validNino)
  private val parsedTaxYear   = TaxYear.fromMtd(validTaxYear)
  private val parsedBenefitId = BenefitId(validBenefitId)
  private val parsedBody      = Def1_AmendBenefitAmountsRequestBody(2050.45, Some(1095.55))

  private def validator(nino: String, taxYear: String, benefitId: String, body: JsValue) =
    new Def1_AmendBenefitAmountsValidator(nino, taxYear, benefitId, body, true)

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody).validateAndWrapResult()
        result shouldBe Right(Def1_AmendBenefitAmountsRequestData(parsedNino, parsedTaxYear, parsedBenefitId, parsedBody))
      }
    }

    "return a single error" when {
      "passed an invalid nino" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator("A12344A", validTaxYear, validBenefitId, validBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }

      "passed an invalid tax year" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator(validNino, "202223", validBenefitId, validBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }

      "passed a tax year with an invalid range" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator(validNino, "2022-24", validBenefitId, validBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }

      "passed a tax year that precedes the minimum" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator(validNino, "2018-19", validBenefitId, validBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }

      "passed an invalid benefitId" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator(validNino, validTaxYear, "invalid", validBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, BenefitIdFormatError))
      }

      "passed an empty body" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator(validNino, validTaxYear, validBenefitId, JsObject.empty).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "passed a body missing a mandatory field" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody.removeProperty("/amount")).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath("/amount")))
      }

      "passed a body with an incorrect formatted amount field" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody.update("/amount", JsNumber(-1))).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.forPathAndRange("/amount", "0", "99999999999.99")))
      }

      "passed a body with an incorrect formatted taxPaid field" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator(validNino, validTaxYear, validBenefitId, validBody.update("/taxPaid", JsNumber(-99999999999.99 - 1))).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.forPathAndRange("/taxPaid", "-99999999999.99", "99999999999.99")))
      }
    }

    "return multiple errors" when {
      "passed multiple invalid fields" in new AppConfigTest {
        val result: Either[ErrorWrapper, AmendBenefitAmountsRequestData] =
          validator("not-a-nino", "not-a-tax-year", "not-a-benefit-id", validBody).validateAndWrapResult()

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
