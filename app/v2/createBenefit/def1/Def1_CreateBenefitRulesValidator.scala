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

package v2.createBenefit.def1

import api.controllers.validators.RulesValidator
import api.controllers.validators.resolvers.{ResolveDateRange, ResolveIsoDate}
import api.models.domain.TaxYear
import api.models.errors.*
import cats.data.Validated
import cats.data.Validated.Invalid
import v2.createBenefit.def1.model.request.Def1_CreateBenefitRequestData
import v2.models.domain.BenefitType.*

import scala.math.Ordered.orderingToOrdered

object Def1_CreateBenefitRulesValidator extends RulesValidator[Def1_CreateBenefitRequestData] {

  private val minYear = 1900
  private val maxYear = 2100

  private val availableBenefitTypes = List(
    statePension,
    statePensionLumpSum,
    employmentSupportAllowance,
    jobSeekersAllowance,
    bereavementAllowance,
    otherStateBenefits,
    incapacityBenefit
  ).map(_.toString)

  def validateBusinessRules(parsed: Def1_CreateBenefitRequestData): Validated[Seq[MtdError], Def1_CreateBenefitRequestData] =
    val taxYear: TaxYear = parsed.taxYear
    combine(
      validateBenefitType(parsed.body.benefitType),
      validateDates(taxYear, parsed.body.startDate, parsed.body.endDate)
    ).onSuccess(parsed)

  private def validateBenefitType(benefitType: String): Validated[Seq[MtdError], Unit] =
    if (availableBenefitTypes.contains(benefitType)) valid else Invalid(List(BenefitTypeFormatError))

  private def validateDates(taxYear: TaxYear, startDate: String, endDate: Option[String]): Validated[Seq[MtdError], Unit] =
    endDate match {
      case Some(endDate) => validateDateRange(taxYear, startDate, endDate)
      case None          => validateStartDate(taxYear, startDate)
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
