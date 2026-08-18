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

package uk.gov.hmrc.icedsubscriptionfrontend.actions

import base.SpecBase
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.icedsubscriptionfrontend.config.MockAppConfig
import uk.gov.hmrc.icedsubscriptionfrontend.services.MockAuthService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class AuthActionNoProfileSpec extends SpecBase with MockAuthService with MockAppConfig {

  val loginUrl  = "someLoginUrl"
  val appNme    = "iced-subscription-frontend"
  val returnUrl = ""

  val authAction =
    new AuthActionNoProfile(stubMessagesControllerComponents().parsers, mockAuthService, mockAppConfig)

  class Controller extends FrontendController(stubMessagesControllerComponents()) {
    def handleRequest(): Action[AnyContent] = authAction { implicit req =>
      Ok("logged in")
    }
  }

  val controller = new Controller

  "AuthActionNoProfile" when {
    "no active session" must {
      "redirect to login" in {
        val continueUrl = s"$loginUrl?continue=%2F&origin=$appNme"
        MockAppConfig.loginUrl returns loginUrl
        MockAppConfig.appName returns appNme
        MockAppConfig.loginReturnBase returns returnUrl

        MockAuthService.authenticateNoProfile returns Future.successful(false)

        val result = controller.handleRequest()(FakeRequest())

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe continueUrl
      }
    }

    "logged in" must {
      "call the block" in {
        MockAuthService.authenticateNoProfile returns Future.successful(true)

        val result = controller.handleRequest()(FakeRequest())

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "logged in"
      }
    }
  }
}
