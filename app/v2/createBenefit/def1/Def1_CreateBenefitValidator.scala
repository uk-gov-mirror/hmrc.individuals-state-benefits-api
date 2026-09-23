/*
 * Copyright 2025 HM Revenue & Customs
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

package v2.createBenefit.def1

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.{ResolveNino, ResolveNonEmptyJsonObject, ResolveTaxYearMinimum}
import api.models.domain.TaxYear
import api.models.errors.MtdError
import cats.data.Validated
import cats.implicits.*
import config.StateBenefitsAppConfig
import play.api.libs.json.JsValue
import v2.createBenefit.def1.Def1_CreateBenefitRulesValidator.validateBusinessRules
import v2.createBenefit.def1.model.request.{Def1_CreateBenefitRequestBody, Def1_CreateBenefitRequestData}
import v2.createBenefit.model.request.CreateBenefitRequestData

import javax.inject.Singleton

@Singleton
class Def1_CreateBenefitValidator(nino: String, taxYear: String, body: JsValue)(implicit stateBenefitsAppConfig: StateBenefitsAppConfig)
    extends Validator[CreateBenefitRequestData] {

  private val resolveJson = new ResolveNonEmptyJsonObject[Def1_CreateBenefitRequestBody]()

  private val resolveTaxYear: ResolveTaxYearMinimum =
    ResolveTaxYearMinimum(TaxYear.ending(stateBenefitsAppConfig.minimumPermittedTaxYear), allowIncompleteTaxYear = false)

  def validate: Validated[Seq[MtdError], Def1_CreateBenefitRequestData] =
    (
      ResolveNino(nino),
      resolveTaxYear(taxYear),
      resolveJson(body)
    ).mapN(Def1_CreateBenefitRequestData.apply).andThen(validateBusinessRules)

}
