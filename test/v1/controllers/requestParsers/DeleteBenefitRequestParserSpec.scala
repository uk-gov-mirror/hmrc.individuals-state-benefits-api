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
import v1.mocks.validators.MockDeleteBenefitValidator
import v1.models.errors._
import v1.models.request.deleteBenefit.{DeleteBenefitRawData, DeleteBenefitRequest}

class DeleteBenefitRequestParserSpec extends UnitSpec {

  val nino: String = "AA123456B"
  val taxYear: String = "2021-22"
  val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val deleteBenefitRawData: DeleteBenefitRawData = DeleteBenefitRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId
  )

  trait Test extends MockDeleteBenefitValidator {
    lazy val parser: DeleteBenefitRequestParser = new DeleteBenefitRequestParser(
      validator = mockDeleteBenefitValidator
    )
  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockDeleteBenefitValidator.validate(deleteBenefitRawData).returns(Nil)

        parser.parseRequest(deleteBenefitRawData) shouldBe
          Right(DeleteBenefitRequest(Nino(nino), taxYear, benefitId))
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        MockDeleteBenefitValidator.validate(deleteBenefitRawData)
          .returns(List(NinoFormatError))

        parser.parseRequest(deleteBenefitRawData) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple validation errors occur (NinoFormatError and TaxYearFormatError errors)" in new Test {
        MockDeleteBenefitValidator.validate(deleteBenefitRawData)
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(deleteBenefitRawData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }

      "multiple validation errors occur (NinoFormatError, TaxYearFormatError and BenefitIdFormatError errors)" in new Test {
        MockDeleteBenefitValidator.validate(deleteBenefitRawData)
          .returns(List(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))

        parser.parseRequest(deleteBenefitRawData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))))
      }
    }
  }
}