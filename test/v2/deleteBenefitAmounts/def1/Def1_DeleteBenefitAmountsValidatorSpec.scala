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

package v2.deleteBenefitAmounts.def1

import api.models.domain.{Nino, TaxYear}
import api.models.errors.*
import api.utils.UnitSpec
import config.MockStateBenefitsAppConfig
import v2.deleteBenefitAmounts.def1.model.request.Def1_DeleteBenefitAmountsRequestData
import v2.deleteBenefitAmounts.model.request.DeleteBenefitAmountsRequestData
import v2.models.domain.BenefitId

class Def1_DeleteBenefitAmountsValidatorSpec extends UnitSpec with MockStateBenefitsAppConfig {

  private implicit val correlationId: String = "1234"

  private val validNino      = "AA123456A"
  private val validTaxYear   = "2023-24"
  private val validBenefitId = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val parsedNino      = Nino(validNino)
  private val parsedTaxYear   = TaxYear.fromMtd(validTaxYear)
  private val parsedBenefitId = BenefitId(validBenefitId)

  private def validator(nino: String, taxYear: String, benefitId: String) = new Def1_DeleteBenefitAmountsValidator(nino, taxYear, benefitId)

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request" in new AppConfigTest {
        val result: Either[ErrorWrapper, DeleteBenefitAmountsRequestData] = validator(validNino, validTaxYear, validBenefitId).validateAndWrapResult()

        result shouldBe Right(Def1_DeleteBenefitAmountsRequestData(parsedNino, parsedTaxYear, parsedBenefitId))
      }
    }

    "return a single error" when {
      "passed an invalid nino" in new AppConfigTest {
        val result: Either[ErrorWrapper, DeleteBenefitAmountsRequestData] = validator("A12344A", validTaxYear, validBenefitId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }

      "passed an invalid tax year" in new AppConfigTest {
        val result: Either[ErrorWrapper, DeleteBenefitAmountsRequestData] = validator(validNino, "202223", validBenefitId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError)
        )
      }

      "passed a tax year with an invalid range" in new AppConfigTest {
        val result: Either[ErrorWrapper, DeleteBenefitAmountsRequestData] = validator(validNino, "2022-24", validBenefitId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError)
        )
      }

      "passed a tax year that precedes the minimum" in new AppConfigTest {
        val result: Either[ErrorWrapper, DeleteBenefitAmountsRequestData] = validator(validNino, "2018-19", validBenefitId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError)
        )
      }

      "passed an invalid benefitId" in new AppConfigTest {
        val result: Either[ErrorWrapper, DeleteBenefitAmountsRequestData] = validator(validNino, validTaxYear, "invalid").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BenefitIdFormatError)
        )
      }
    }

    "return multiple errors" when {
      "passed multiple invalid fields" in new AppConfigTest {
        val result: Either[ErrorWrapper, DeleteBenefitAmountsRequestData] =
          validator("not-a-nino", "not-a-tax-year", "not-a-benefit-id").validateAndWrapResult()

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
