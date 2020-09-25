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

package v1.controllers.requestParsers.validators

import config.AppConfig
import javax.inject.Inject
import utils.CurrentDateTime
import v1.controllers.requestParsers.validators.validations._
import v1.models.errors.MtdError
import v1.models.request.AmendBenefit.{AmendBenefitRawData, AmendBenefitRequestBody}

class AmendBenefitValidator @Inject()(implicit currentDateTime: CurrentDateTime, appConfig: AppConfig) extends Validator[AmendBenefitRawData] {

  private val validationSet = List(parameterFormatValidation, parameterRuleValidation, bodyFormatValidator, bodyValueValidator)

  override def validate(data: AmendBenefitRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }

  private def parameterFormatValidation: AmendBenefitRawData => List[List[MtdError]] = (data: AmendBenefitRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear),
      BenefitIdValidation.validate(data.benefitId)
    )
  }

  private def parameterRuleValidation: AmendBenefitRawData => List[List[MtdError]] = { data =>
    List(
      TaxYearNotSupportedValidation.validate(data.taxYear),
      TaxYearNotEndedValidation.validate(data.taxYear)
    )
  }

  private def bodyFormatValidator: AmendBenefitRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendBenefitRequestBody](data.body.json)
    )
  }

  private def bodyValueValidator: AmendBenefitRawData => List[List[MtdError]] = (data: AmendBenefitRawData) => {
    val requestBodyData: AmendBenefitRequestBody = data.body.json.as[AmendBenefitRequestBody]

    List(
      StateBenefitsDateValidation.validate(requestBodyData.startDate, requestBodyData.endDate, data.taxYear)
    )
  }
}
