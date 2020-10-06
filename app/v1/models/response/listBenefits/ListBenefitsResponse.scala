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

      // Common links for both HMRC and CUSTOM created state benefits
      val commonLinks = if (stateBenefit.hasAmounts) {
        Seq(
          retrieveSingleBenefit(appConfig, nino, taxYear, stateBenefit.benefitId),
          updateBenefitAmounts(appConfig, nino, taxYear, stateBenefit.benefitId),
          deleteBenefitAmounts(appConfig, nino, taxYear, stateBenefit.benefitId)
        )
      } else {
        Seq(
          retrieveSingleBenefit(appConfig, nino, taxYear, stateBenefit.benefitId),
          updateBenefitAmounts(appConfig, nino, taxYear, stateBenefit.benefitId)
        )
      }

      // Links specific to the type (HMRC/CUSTOM) state benefit
      val links = stateBenefit.createdBy match {
        case Some("CUSTOM") => commonLinks ++ Seq(deleteBenefit(appConfig, nino, taxYear, stateBenefit.benefitId),
          updateBenefit(appConfig, nino, taxYear, stateBenefit.benefitId))
        case _ => commonLinks :+ ignoreBenefit(appConfig, nino, taxYear, stateBenefit.benefitId)
      }

      // Differentiate the links based on the call list/single by benefitId passed in the request
      // for list only retrieve (self)
      data.benefitId match {
        case None => Seq(retrieveSingleBenefit(appConfig, nino, taxYear, stateBenefit.benefitId))
        case _ => links
      }
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

  implicit def writes[B: Writes]: OWrites[ListBenefitsResponse[B]] = Json.writes[ListBenefitsResponse[B]]

  // Added temporary field "createdBy" to identify the type of state benefits
  // Only used in json reads
  def readJson[T](createdBy: String)(implicit rds: Reads[Seq[T]]): Reads[Seq[T]] = (json: JsValue) => {
    json
      .validate[JsValue]
      .flatMap(
        readJson => {
          Json.toJson(readJson.as[JsObject].fields.flatMap {
            case (field, arr: JsArray) =>
              arr.value.map {
                element =>
                  element.as[JsObject] + ("benefitType" -> Json.toJson(field)) + ("createdBy" -> Json.toJson(createdBy))
              }
            case (field, obj: JsObject) =>
              Seq(obj.as[JsObject] + ("benefitType" -> Json.toJson(field)) + ("createdBy" -> Json.toJson(createdBy)))
            case (_, _) => Seq.empty
          }).validate[Seq[T]]})
  }

  implicit def reads[B: Reads]: Reads[ListBenefitsResponse[B]] = for {
    stateBenefits <- (__ \ "stateBenefits").readNullable(readJson[B](createdBy = "HMRC")).mapEmptySeqToNone
    customerAddedStateBenefits <- (__ \ "customerAddedStateBenefits").readNullable(readJson[B](createdBy = "CUSTOM")).mapEmptySeqToNone
  } yield ListBenefitsResponse[B](stateBenefits, customerAddedStateBenefits)

}

case class ListBenefitsHateoasData(nino: String, taxYear: String, benefitId: Option[String]) extends HateoasData
