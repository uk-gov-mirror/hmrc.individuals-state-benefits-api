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

package v2.amendBenefit.def1

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.*
import api.models.domain.TaxYear
import api.models.errors.*
import cats.data.Validated
import cats.implicits.catsSyntaxTuple4Semigroupal
import config.StateBenefitsAppConfig
import play.api.libs.json.JsValue
import v2.amendBenefit.def1.model.request.{Def1_AmendBenefitRequestBody, Def1_AmendBenefitRequestData}
import v2.amendBenefit.model.request.AmendBenefitRequestData

import javax.inject.Singleton
import scala.math.Ordered.orderingToOrdered

@Singleton
class Def1_AmendBenefitValidator(nino: String, taxYear: String, benefitId: String, body: JsValue)(implicit
    stateBenefitsAppConfig: StateBenefitsAppConfig)
    extends Validator[AmendBenefitRequestData] {

  private val minYear = 1900
  private val maxYear = 2100

  private val resolveJson = new ResolveNonEmptyJsonObject[Def1_AmendBenefitRequestBody]()

  private val resolveTaxYear = ResolveDetailedTaxYear(minimumTaxYear = TaxYear.ending(stateBenefitsAppConfig.minimumPermittedTaxYear))

  def validate: Validated[Seq[MtdError], Def1_AmendBenefitRequestData] =
    (
      ResolveNino(nino),
      resolveTaxYear(taxYear),
      ResolveBenefitId(benefitId),
      resolveJson(body)
    ).mapN(Def1_AmendBenefitRequestData.apply).andThen(validateBusinessRules)

  private def validateBusinessRules(parsed: Def1_AmendBenefitRequestData): Validated[Seq[MtdError], Def1_AmendBenefitRequestData] = {
    import parsed.body.*

    val taxYear: TaxYear = parsed.taxYear

    val validatedDates: Validated[Seq[MtdError], Unit] = endDate match {
      case Some(endDate) => validateDateRange(taxYear, startDate, endDate)
      case None          => validateStartDate(taxYear, startDate)
    }

    validatedDates.map(_ => parsed)
  }

  private def validateStartDate(taxYear: TaxYear, startDate: String): Validated[Seq[MtdError], Unit] =
    ResolveIsoDate.withMinMaxCheck(startDate, StartDateFormatError, StartDateFormatError).andThen { date =>
      Validated.cond(date <= taxYear.endDate, (), List(RuleStartDateAfterTaxYearEndError))
    }

  private def validateDateRange(taxYear: TaxYear, startDate: String, endDate: String): Validated[Seq[MtdError], Unit] =
    ResolveDateRange().withYearsLimitedTo(minYear, maxYear)(startDate -> endDate).andThen { dateRange =>
      combine(
        Validated.cond(dateRange.startDate <= taxYear.endDate, (), List(RuleStartDateAfterTaxYearEndError)),
        Validated.cond(dateRange.endDate >= taxYear.startDate, (), List(RuleEndDateBeforeTaxYearStartError))
      )
    }

}
