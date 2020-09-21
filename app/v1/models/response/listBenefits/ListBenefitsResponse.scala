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

import cats.Functor
import config.AppConfig
import play.api.libs.json._
import utils.JsonUtils
import v1.hateoas.{HateoasLinks, HateoasListLinksFactory}
import v1.models.hateoas.{HateoasData, Link}

case class ListBenefitsResponse[B](stateBenefits: Option[Seq[B]],
                                customerAddedStateBenefits: Option[Seq[B]])

object ListBenefitsResponse extends HateoasLinks with JsonUtils {

  implicit object ListBenefitsLinksFactory extends HateoasListLinksFactory[ListBenefitsResponse, StateBenefit, ListBenefitsHateoasData] {

    override def itemLinks(appConfig: AppConfig, data: ListBenefitsHateoasData, stateBenefit: StateBenefit): Seq[Link] = {
      import data._
      Seq(
        retrieveSingleBenefit(appConfig, nino, taxYear, stateBenefit.benefitId)
      )
    }

    override def links(appConfig: AppConfig, data: ListBenefitsHateoasData): Seq[Link] = {
      import data._
      Seq(
        addBenefit(appConfig, nino, taxYear),
        listBenefits(appConfig, nino, taxYear)
      )
    }
  }

  implicit object ResponseFunctor extends Functor[ListBenefitsResponse] {
    override def map[A, B](fa: ListBenefitsResponse[A])(f: A => B): ListBenefitsResponse[B] =
      ListBenefitsResponse(
        fa.stateBenefits.map(x => x.map(f)), fa.customerAddedStateBenefits.map(y => y.map(f)))
  }

  implicit def writes[B: Writes]: OWrites[ListBenefitsResponse[B]] = (response: ListBenefitsResponse[B]) => Json.obj(
    "stateBenefits" -> response.stateBenefits,
    "customerAddedStateBenefits" -> response.customerAddedStateBenefits
  )

  def readJson[T]()(implicit rds: Reads[Seq[T]]): Reads[Seq[T]] = (json: JsValue) => {
    json
      .validate[JsValue]
      .flatMap(
        readJson => {
          Json.toJson(readJson.as[JsObject].fields.flatMap {
            case (field, arr: JsArray) =>
              arr.value.map {
                element =>
                  element.as[JsObject] + ("benefitType" -> Json.toJson(field))
              }
            case (field, obj: JsObject) =>
              Seq(obj.as[JsObject] + ("benefitType" -> Json.toJson(field)))
            case (_, _) => Seq.empty
          }).validate[Seq[T]]})
  }

  implicit def reads[B: Reads]: Reads[ListBenefitsResponse[B]] = for {
    stateBenefits <- (__ \ "stateBenefits").readNullable(readJson[B]())
    customerAddedStateBenefits <- (__ \ "customerAddedStateBenefits").readNullable(readJson[B]())
  } yield ListBenefitsResponse[B](stateBenefits, customerAddedStateBenefits)

}

case class ListBenefitsHateoasData(nino: String, taxYear: String) extends HateoasData
