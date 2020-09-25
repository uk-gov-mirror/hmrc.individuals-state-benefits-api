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

package v1.models.response

import config.AppConfig
import play.api.libs.json.{Json, OFormat}
import v1.hateoas.{HateoasLinks, HateoasLinksFactory}
import v1.models.hateoas.{HateoasData, Link}

case class AddBenefitResponse(benefitId: String)

object AddBenefitResponse extends HateoasLinks {

  implicit val format: OFormat[AddBenefitResponse] = Json.format[AddBenefitResponse]

  implicit object AddBenefitLinksFactory extends HateoasLinksFactory[AddBenefitResponse, AddBenefitHateoasData] {
    override def links(appConfig: AppConfig, data: AddBenefitHateoasData): Seq[Link] = {
      import data._
      Seq(
        listBenefits(appConfig, nino, taxYear),
        updateBenefit(appConfig, nino, taxYear, benefitId),
        deleteBenefit(appConfig, nino, taxYear, benefitId)
      )
    }
  }
}

case class AddBenefitHateoasData(nino: String, taxYear: String, benefitId: String) extends HateoasData
