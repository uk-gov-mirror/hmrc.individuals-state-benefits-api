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
import play.api.libs.json.{Format, Json}
import v1.hateoas.{HateoasLinks, HateoasLinksFactory}
import v1.models.hateoas.{HateoasData, Link}

case class AddStateBenefitsResponse(benefitId: String)

object AddStateBenefitsResponse extends HateoasLinks {

  implicit val format: Format[AddStateBenefitsResponse] = Json.format[AddStateBenefitsResponse]

  implicit object AddStateBenefitsLinksFactory extends HateoasLinksFactory[AddStateBenefitsResponse, AddStateBenefitsHateoasData] {
    override def links(appConfig: AppConfig, data: AddStateBenefitsHateoasData): Seq[Link] = {
      import data._
      Seq(
        listStateBenefits(appConfig, nino, taxYear),
        updateStateBenefit(appConfig, nino, taxYear, benefitId),
        deleteStateBenefit(appConfig, nino, taxYear, benefitId)
      )
    }
  }
}

case class AddStateBenefitsHateoasData(nino: String, taxYear: String, benefitId: String) extends HateoasData
