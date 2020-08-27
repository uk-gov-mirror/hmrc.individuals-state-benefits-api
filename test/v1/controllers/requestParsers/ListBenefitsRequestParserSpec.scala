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
import v1.mocks.validators.MockListBenefitsValidator
import v1.models.errors._
import v1.models.request.listBenefits.{ListBenefitsRawData, ListBenefitsRequest}

class ListBenefitsRequestParserSpec extends UnitSpec {

  val nino: String = "AA123456B"
  val taxYear: String = "2020-21"

  val listBenefitsRawData: ListBenefitsRawData = ListBenefitsRawData(
    nino = nino,
    taxYear = taxYear
  )

  trait Test extends MockListBenefitsValidator {
    lazy val parser: ListBenefitsRequestParser = new ListBenefitsRequestParser(
      validator = mockListBenefitsValidator
    )
  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockListBenefitsValidator.validate(listBenefitsRawData).returns(Nil)

        parser.parseRequest(listBenefitsRawData) shouldBe
          Right(ListBenefitsRequest(Nino(nino), taxYear))
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        MockListBenefitsValidator.validate(listBenefitsRawData)
          .returns(List(NinoFormatError))

        parser.parseRequest(listBenefitsRawData) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple validation errors occur (NinoFormatError and TaxYearFormatError errors)" in new Test {
        MockListBenefitsValidator.validate(listBenefitsRawData)
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(listBenefitsRawData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }
  }
}