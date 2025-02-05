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

package v1.connectors

import config.AppConfig

import javax.inject.{Inject, Singleton}
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v1.connectors.DownstreamUri.DesUri
import v1.models.request.listBenefits.ListBenefitsRequest
import v1.models.response.listBenefits.{ListBenefitsResponse, StateBenefit}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ListBenefitsConnector @Inject()(val http: HttpClient,
                                      val appConfig: AppConfig) extends BaseDesConnector {

  def listBenefits(request: ListBenefitsRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    correlationId: String): Future[DownstreamOutcome[ListBenefitsResponse[StateBenefit]]] = {

    import v1.connectors.httpparsers.StandardDesHttpParser._
    implicit val successCode: SuccessCode = SuccessCode(Status.OK)

    val nino = request.nino.nino
    val taxYear = request.taxYear

    val queryParams = Map("benefitId" -> request.benefitId).collect {
        case (key, Some(value)) => key -> value
      }

    getWithQueryParams(
      DesUri[ListBenefitsResponse[StateBenefit]](s"income-tax/income/state-benefits/$nino/$taxYear"),
      queryParams
    )
  }
}

