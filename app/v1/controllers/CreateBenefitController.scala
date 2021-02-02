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

package v1.controllers

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.{IdGenerator, Logging}
import v1.controllers.requestParsers.CreateBenefitRequestParser
import v1.hateoas.HateoasFactory
import v1.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import v1.models.errors._
import v1.models.request.createBenefit.CreateBenefitRawData
import v1.models.response.AddBenefitHateoasData
import v1.services.{AuditService, CreateBenefitService, EnrolmentsAuthService, MtdIdLookupService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateBenefitController @Inject()(val authService: EnrolmentsAuthService,
                                        val lookupService: MtdIdLookupService,
                                        requestParser: CreateBenefitRequestParser,
                                        service: CreateBenefitService,
                                        auditService: AuditService,
                                        hateoasFactory: HateoasFactory,
                                        cc: ControllerComponents,
                                        idGenerator: IdGenerator)(implicit ec: ExecutionContext)

  extends AuthorisedController(cc) with BaseController with Logging {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(
      controllerName = "CreateStateBenefitController",
      endpointName = "CreateStateBenefit"
    )

  def createStateBenefit(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      implicit val correlationId: String = idGenerator.getCorrelationId
      logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
        s"with correlationId : $correlationId")

      val rawData: CreateBenefitRawData = CreateBenefitRawData(
        nino = nino,
        taxYear = taxYear,
        body = AnyContentAsJson(request.body)
      )
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](requestParser.parseRequest(rawData))
          serviceResponse <- EitherT(service.addBenefit(parsedRequest))
          hateoasResponse <- EitherT.fromEither[Future](
            hateoasFactory
              .wrap(
                serviceResponse.responseData,
                AddBenefitHateoasData(nino, taxYear, serviceResponse.responseData.benefitId)
              )
              .asRight[ErrorWrapper])
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          auditSubmission(
            GenericAuditDetail(request.userDetails, Map("nino" -> nino, "taxYear" -> taxYear), Some(request.body),
              serviceResponse.correlationId, AuditResponse(httpStatus = OK, response = Right(Some(Json.toJson(hateoasResponse))))
            )
          )
          Ok(Json.toJson(hateoasResponse))
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val resCorrelationId = errorWrapper.correlationId
        val result = errorResult(errorWrapper).withApiHeaders(resCorrelationId)
        logger.warn(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $resCorrelationId")

        auditSubmission(
          GenericAuditDetail(request.userDetails, Map("nino" -> nino, "taxYear" -> taxYear), Some(request.body),
            correlationId, AuditResponse(httpStatus = result.header.status, response = Left(errorWrapper.auditErrors))
          )
        )
        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | RuleTaxYearRangeInvalidError |
           RuleTaxYearNotSupportedError | RuleTaxYearNotEndedError | BenefitTypeFormatError |
           StartDateFormatError | EndDateFormatError | RuleEndDateBeforeStartDateError |
           RuleStartDateAfterTaxYearEndError | RuleEndDateBeforeTaxYearStartError |
           CustomMtdError(RuleIncorrectOrEmptyBodyError.code)
      => BadRequest(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: GenericAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("CreateStateBenefit", "create-state-benefit", details)
    auditService.auditEvent(event)
  }

}
