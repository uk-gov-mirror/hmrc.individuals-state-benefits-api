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

package v1.models.hateoas

object RelType {
  val AMEND_SAMPLE_REL = "amend-sample-rel"
  val RETRIEVE_SAMPLE_REL = "retrieve-sample-rel"
  val DELETE_SAMPLE_REL = "delete-sample-rel"

  val ADD_STATE_BENEFIT = "add-state-benefit"
  val LIST_STATE_BENEFITS = "list-state-benefits"
  val UPDATE_STATE_BENEFIT = "update-state-benefit"
  val DELETE_STATE_BENEFIT = "delete-state-benefit"
  val UPDATE_STATE_BENEFIT_AMOUNTS = "update-state-benefit-amounts"
  val DELETE_STATE_BENEFIT_AMOUNTS = "delete-state-benefit-amounts"
  val IGNORE_STATE_BENEFIT = "ignore-state-benefit"

  val SELF = "self"
}
