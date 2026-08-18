/*
 * Copyright 2025 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.icedsubscriptionfrontend.config.AppConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.icedsubscriptionfrontend.views.html.SignedOutPage
import uk.gov.hmrc.icedsubscriptionfrontend.controllers

@Singleton
class SignOutController @Inject()(
  signedOutPage: SignedOutPage,
  mcc: MessagesControllerComponents)(
  using val appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  def signOut: Action[AnyContent] = doSignOut(controllers.routes.SignOutController.signedOut())

  def signOutToRestart: Action[AnyContent] = doSignOut(controllers.routes.SubscriptionController.start)

  private def doSignOut(continue: Call): Action[AnyContent] =
    Action(
      Redirect(
        appConfig.basGatewaySignOutUrl,
        Map("continue" -> Seq(s"${appConfig.loginReturnBase}$continue"), "origin" -> Seq(appConfig.appName))
      )
    )

  def signedOut(): Action[AnyContent] = Action { implicit request =>
    Ok(signedOutPage())
  }
}
