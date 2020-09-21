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

package v1.models.response.listBenefits

import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import play.api.libs.functional.syntax._
import utils.JsonUtils

case class StateBenefit(benefitType: String,
                        dateIgnored: Option[String] = None,
                        submittedOn: Option[String],
                        benefitId: String,
                        startDate: String,
                        endDate: Option[String],
                        amount: Option[BigDecimal],
                        taxPaid: Option[BigDecimal])

object StateBenefit extends JsonUtils {

  implicit val writes: OWrites[StateBenefit] = Json.writes[StateBenefit]

  implicit val reads: Reads[StateBenefit] =  (
    (JsPath \ "benefitType").read[String] and
      (JsPath \ "dateIgnored").readNullable[String] and
      (JsPath \ "submittedOn").readNullable[String] and
      (JsPath \ "benefitId").read[String] and
      (JsPath \ "startDate").read[String] and
      (JsPath \ "endDate").readNullable[String] and
      (JsPath \ "amount").readNullable[BigDecimal] and
      (JsPath \ "taxPaid").readNullable[BigDecimal]
    )(StateBenefit.apply _)

}
