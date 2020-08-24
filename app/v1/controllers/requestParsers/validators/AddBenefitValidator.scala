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
import v1.models.request.addStateBenefit.{AddStateBenefitBody, AddStateBenefitRawData}

class AddBenefitValidator @Inject()(implicit currentDateTime: CurrentDateTime, appConfig: AppConfig) extends Validator[AddStateBenefitRawData] {

  private val validationSet = List(parameterFormatValidation, parameterRuleValidation, bodyFormatValidation, bodyParameterValidation)

  private def parameterFormatValidation: AddStateBenefitRawData => List[List[MtdError]] = (data: AddStateBenefitRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear),
    )
  }

  private def parameterRuleValidation: AddStateBenefitRawData => List[List[MtdError]] = { data =>
    List(
      TaxYearNotSupportedValidation.validate(data.taxYear),
      TaxYearNotEndedValidation.validate(data.taxYear)
    )
  }

  private def bodyFormatValidation: AddStateBenefitRawData => List[List[MtdError]] = { data =>

    List(
      JsonFormatValidation.validate[AddStateBenefitBody](data.body.json)
    )
  }

  private def bodyParameterValidation: AddStateBenefitRawData => List[List[MtdError]] = { data =>
    val body = data.body.json.as[AddStateBenefitBody]

    List(
      StateBenefitsDateValidation.validate(body.startDate, body.endDate, data.taxYear),
      BenefitTypeValidation.validate(body.benefitType)
    )
  }

  override def validate(data: AddStateBenefitRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }
}
