/*
 * Copyright 2021 HM Revenue & Customs
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
import v1.mocks.validators.MockIgnoreBenefitValidator
import v1.models.errors._
import v1.models.request.ignoreBenefit.{IgnoreBenefitRawData, IgnoreBenefitRequest}

class IgnoreBenefitRequestParserSpec extends UnitSpec {

  private val nino: String = "AA123456B"
  private val taxYear: String = "2021-22"
  private val benefitId: String = "b1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val ignoreBenefitRawData = IgnoreBenefitRawData(
    nino = nino,
    taxYear = taxYear,
    benefitId = benefitId
  )

  private val ignoreBenefitRequest = IgnoreBenefitRequest (
    nino = Nino(nino),
    taxYear = taxYear,
    benefitId = benefitId
  )

  trait Test extends MockIgnoreBenefitValidator {
    lazy val parser: IgnoreBenefitRequestParser = new IgnoreBenefitRequestParser(
      validator = mockIgnoreBenefitValidator
    )
  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockIgnoreBenefitValidator.validate(ignoreBenefitRawData).returns(Nil)
        parser.parseRequest(ignoreBenefitRawData) shouldBe Right(ignoreBenefitRequest)
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        MockIgnoreBenefitValidator.validate(ignoreBenefitRawData.copy(nino = "notANino"))
          .returns(List(NinoFormatError))

        parser.parseRequest(ignoreBenefitRawData.copy(nino = "notANino")) shouldBe
          Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }

      "multiple path parameter validation errors occur" in new Test {
        MockIgnoreBenefitValidator.validate(ignoreBenefitRawData.copy(nino = "notANino", taxYear = "notATaxYear", benefitId = "notABenefitId"))
          .returns(List(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))

        parser.parseRequest(ignoreBenefitRawData.copy(nino = "notANino", taxYear = "notATaxYear", benefitId = "notABenefitId")) shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError, BenefitIdFormatError))))
      }
    }
  }
}
