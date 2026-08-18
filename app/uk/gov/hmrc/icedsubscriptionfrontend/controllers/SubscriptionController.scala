/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.icedsubscriptionfrontend.controllers

import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.icedsubscriptionfrontend.actions.{AuthActionWithProfile, UserType}
import uk.gov.hmrc.icedsubscriptionfrontend.audit.{AuditEvent, AuditHandler}
import uk.gov.hmrc.icedsubscriptionfrontend.config.AppConfig
import uk.gov.hmrc.icedsubscriptionfrontend.views.html.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class SubscriptionController @Inject()(
  appConfig: AppConfig,
  authAction: AuthActionWithProfile,
  mcc: MessagesControllerComponents,
  alreadyEnrolledPage: AlreadyEnrolledPage,
  nonOrgGgwPage: NonOrgGgwPage,
  individualPage: BadUserIndividualPage,
  agentPage: BadUserAgentPage,
  verifyUserPage: BadUserVerifyPage,
  wrongCredentialRolePage: WrongCredentialRolePage,
  auditHandler: AuditHandler)
    extends FrontendController(mcc)
    with I18nSupport {

  given config: AppConfig = appConfig

  def start: Action[AnyContent] = authAction { implicit request =>
    import UserType.*

    request.userType match {
      case AlreadyEnrolled(eoriNumber) =>
        auditHandler.audit(AuditEvent.forAlreadyEnrolledTrader(eoriNumber))
        Ok(alreadyEnrolledPage(eoriNumber))
      case NotEnrolled                   => Redirect(appConfig.eoriCommonComponentStartUrl)
      case WrongCredentialRole           => Ok(wrongCredentialRolePage())
      case UnsupportedAffinityIndividual => Ok(individualPage())
      case UnsupportedAffinityAgent      => Ok(agentPage())
      case UnsupportedVerifyUser         => Ok(verifyUserPage())
      case NonGovernmentGatewayUser      => Ok(nonOrgGgwPage())
    }
  }
}
