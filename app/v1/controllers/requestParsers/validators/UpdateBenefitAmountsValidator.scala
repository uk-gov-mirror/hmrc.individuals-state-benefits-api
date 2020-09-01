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
import v1.models.request.updateBenefitAmounts.{UpdateBenefitAmountsRawData, UpdateBenefitAmountsRequestBody}

class UpdateBenefitAmountsValidator @Inject()(implicit currentDateTime: CurrentDateTime, appConfig: AppConfig)
  extends Validator[UpdateBenefitAmountsRawData] with ValueFormatErrorMessages {

  private val validationSet = List(parameterFormatValidation, parameterRuleValidation, bodyFormatValidator, bodyValueValidator)

  override def validate(data: UpdateBenefitAmountsRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }

  private def parameterFormatValidation: UpdateBenefitAmountsRawData => List[List[MtdError]] = (data: UpdateBenefitAmountsRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear),
      BenefitIdValidation.validate(data.benefitId)
    )
  }

  private def parameterRuleValidation: UpdateBenefitAmountsRawData => List[List[MtdError]] = { data =>
    List(
      TaxYearNotSupportedValidation.validate(data.taxYear),
      TaxYearNotEndedValidation.validate(data.taxYear)
    )
  }

  private def bodyFormatValidator: UpdateBenefitAmountsRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[UpdateBenefitAmountsRequestBody](data.body.json)
    )
  }

  private def bodyValueValidator: UpdateBenefitAmountsRawData => List[List[MtdError]] = (data: UpdateBenefitAmountsRawData) => {
    val requestBodyData: UpdateBenefitAmountsRequestBody = data.body.json.as[UpdateBenefitAmountsRequestBody]

    List(
      DecimalValueValidation.validate(
        amount = requestBodyData.amount,
        path = "/amount"
      ),
      DecimalValueValidation.validateOptional(
        amount = requestBodyData.taxPaid,
        path = "/taxPaid",
        minValue = -99999999999.99,
        message = BIG_DECIMAL_MINIMUM_INCLUSIVE
      )
    )
  }
}