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

package v1.controllers

import cats.data.EitherT
import cats.implicits._
import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import play.mvc.Http.MimeTypes
import utils.Logging
import v1.controllers.requestParsers.AmendBenefitAmountsRequestParser
import v1.hateoas.AmendHateoasBodies
import v1.models.errors._
import v1.models.request.AmendBenefitAmounts.AmendBenefitAmountsRawData
import v1.services.{EnrolmentsAuthService, MtdIdLookupService, AmendBenefitAmountsService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendBenefitAmountsController @Inject()(val authService: EnrolmentsAuthService,
                                              val lookupService: MtdIdLookupService,
                                              appConfig: AppConfig,
                                              requestParser: AmendBenefitAmountsRequestParser,
                                              service: AmendBenefitAmountsService,
                                              cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController with Logging with AmendHateoasBodies {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(
      controllerName = "AmendBenefitAmountsController",
      endpointName = "amendBenefitAmounts"
    )

  def amendBenefitAmounts(nino: String, taxYear: String, benefitId: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      val rawData: AmendBenefitAmountsRawData = AmendBenefitAmountsRawData(
        nino = nino,
        taxYear = taxYear,
        benefitId = benefitId,
        body = AnyContentAsJson(request.body)
      )

      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](requestParser.parseRequest(rawData))
          serviceResponse <- EitherT(service.updateBenefitAmounts(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          Ok(amendBenefitAmountsHateoasBody(appConfig, nino, taxYear, benefitId))
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result = errorResult(errorWrapper).withApiHeaders(correlationId)

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | BenefitIdFormatError |
           RuleTaxYearNotSupportedError | RuleTaxYearRangeInvalidError | RuleTaxYearNotEndedError |
           CustomMtdError(RuleIncorrectOrEmptyBodyError.code) |
           CustomMtdError(ValueFormatError.code)
      => BadRequest(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }
}