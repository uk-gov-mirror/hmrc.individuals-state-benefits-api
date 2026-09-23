/*
 * Copyright 2023 HM Revenue & Customs
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
import api.models.errors.{MtdError, StartDateFormatError}
import cats.data.Validated
import cats.data.Validated.Invalid
import common.errors.BenefitTypeFormatError
import v2.createBenefit.def1.model.request.Def1_CreateBenefitRequestData
import v2.models.domain.BenefitType.*

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

  def validateBusinessRules(taxYear: String, parsed: Def1_CreateBenefitRequestData): Validated[Seq[MtdError], Def1_CreateBenefitRequestData] =
    combine(
      validateBenefitType(parsed.body.benefitType),
      validateDates(taxYear.toInt, parsed.body.startDate, parsed.body.endDate)
    ).onSuccess(parsed)

  private def validateBenefitType(benefitType: String): Validated[Seq[MtdError], Unit] =
    if (availableBenefitTypes.contains(benefitType)) valid else Invalid(List(BenefitTypeFormatError))

  private def validateDates(taxYear: Int, startDate: String, endDate: Option[String]): Validated[Seq[MtdError], Unit] =
    endDate match {
      case Some(endDate) => {
        ResolveDateRange().withYearsLimitedTo(minYear, maxYear)(startDate -> endDate).toUnit
        ResolveDateRange().startDateAfterEndOfTaxYear(taxYear, startDate).toUnit
        ResolveDateRange().endDateBeforestartOfTaxYear(taxYear, endDate).toUnit
      }
      case None => {
        ResolveIsoDate.withMinMaxCheck(startDate, StartDateFormatError, StartDateFormatError).toUnit
        ResolveDateRange().startDateAfterEndOfTaxYear(taxYear, startDate).toUnit
      }
    }

}
