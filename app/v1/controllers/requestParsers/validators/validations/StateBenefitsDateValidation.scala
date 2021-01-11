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

package v1.controllers.requestParsers.validators.validations

import java.time.LocalDate

import v1.models.domain.DesTaxYear
import v1.models.errors._

object StateBenefitsDateValidation {

  def validate(startDate: String, endDate: Option[String], taxYear: String): List[MtdError] = {

    lazy val taxYearStartDate: LocalDate = LocalDate.parse(taxYear.take(4) + "-04-06", dateFormat)
    lazy val taxYearEndDate: LocalDate = LocalDate.parse(DesTaxYear.fromMtd(taxYear) + "-04-05", dateFormat)

    val formatErrors: List[MtdError] = List(
      Some(DateFormatValidation.validate(startDate, StartDateFormatError)),
      endDate.map(DateFormatValidation.validate(_, EndDateFormatError))
    ).flatten.flatten

    formatErrors match {
      case Nil =>
        val start = LocalDate.parse(startDate, dateFormat)
        val end = endDate.map(LocalDate.parse(_, dateFormat))

        List(
          Some(checkDateOrder(start, taxYearEndDate, RuleStartDateAfterTaxYearEndError)),
          end.map(checkDateOrder(taxYearStartDate, _, RuleEndDateBeforeTaxYearStartError)),
          end.map(checkDateOrder(start, _, RuleEndDateBeforeStartDateError))
        ).flatten.flatten

      case List(StartDateFormatError) =>
        val end = endDate.map(LocalDate.parse(_, dateFormat))

        List(
          Some(List(StartDateFormatError)),
          end.map(checkDateOrder(taxYearStartDate, _, RuleEndDateBeforeTaxYearStartError))
        ).flatten.flatten

      case List(endDateFormatError) =>
        val start = LocalDate.parse(startDate, dateFormat)

        List(endDateFormatError) ++ checkDateOrder(start, taxYearEndDate, RuleStartDateAfterTaxYearEndError)

      case other => other
    }
  }

  private def checkDateOrder(start: LocalDate, end: LocalDate, error: MtdError): List[MtdError] =
    if (start.isAfter(end)) List(error) else NoValidationErrors
}

