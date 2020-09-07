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

import config.AppConfig
import play.api.libs.json._
import v1.hateoas.{HateoasLinks, HateoasLinksFactory}
import v1.models.hateoas.{HateoasData, Link}

case class ListBenefitsResponse(stateBenefits: StateBenefits, customerAddedStateBenefits: CustomerAddedStateBenefits)

object ListBenefitsResponse extends HateoasLinks {

  implicit val formatListBenefitsResponse: OFormat[ListBenefitsResponse] = Json.format[ListBenefitsResponse]

  implicit object ListBenefitsLinksFactory extends HateoasLinksFactory[ListBenefitsResponse, ListBenefitsHateoasData] {
    override def links(appConfig: AppConfig, data: ListBenefitsHateoasData): Seq[Link] = {
      import data._
      Seq(
        addBenefit(appConfig, nino, taxYear),
        listBenefits(appConfig, nino, taxYear)
      )
    }
  }
}

case class ListBenefitsHateoasData(nino: String, taxYear: String) extends HateoasData
