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

package v1.controllers.requestParsers

import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v1.mocks.validators.MockDeleteBenefitsValidator
import v1.models.errors._
import v1.models.request.deleteBenefits.{DeleteBenefitsRawData, DeleteBenefitsRequest}

class DeleteBenefitsRequestParserSpec extends UnitSpec {

  val nino: String = "AA123456B"
  val taxYear: String = "2021-22"
  val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val deleteBenefitsRawData: DeleteBenefitsRawData = DeleteBenefitsRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId
  )

  trait Test extends MockDeleteBenefitsValidator {
    lazy val parser: DeleteBenefitsRequestParser = new DeleteBenefitsRequestParser(
      validator = mockDeleteBenefitsValidator
    )
  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockDeleteBenefitsValidator.validate(deleteBenefitsRawData).returns(Nil)

        parser.parseRequest(deleteBenefitsRawData) shouldBe
          Right(DeleteBenefitsRequest(Nino(nino), taxYear, benefitId))
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        MockDeleteBenefitsValidator.validate(deleteBenefitsRawData)
          .returns(List(NinoFormatError))

        parser.parseRequest(deleteBenefitsRawData) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple validation errors occur (NinoFormatError and TaxYearFormatError errors)" in new Test {
        MockDeleteBenefitsValidator.validate(deleteBenefitsRawData)
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(deleteBenefitsRawData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }

      "multiple validation errors occur (NinoFormatError, TaxYearFormatError and BenefitIdFormatError errors)" in new Test {
        MockDeleteBenefitsValidator.validate(deleteBenefitsRawData)
          .returns(List(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))

        parser.parseRequest(deleteBenefitsRawData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))))
      }
    }
  }
}