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

import base.SpecBase
import play.api.mvc.Result
import play.api.test.Helpers.*
import uk.gov.hmrc.icedsubscriptionfrontend.actions.AuthActionNoProfile
import uk.gov.hmrc.icedsubscriptionfrontend.config.MockAppConfig
import uk.gov.hmrc.icedsubscriptionfrontend.services.MockAuthService
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class KeepAliveControllerSpec extends SpecBase with MockAppConfig with MockAuthService {

  val appNme    = "iced-subscription-frontend"
  val returnUrl = ""

  val authAction = new AuthActionNoProfile(stubMessagesControllerComponents().parsers, mockAuthService, mockAppConfig)

  private val controller =
    new KeepAliveController(authAction, stubMessagesControllerComponents())

  "KeepAliveControllerSpec.keepAlive" when {
    "logged in" must {
      "return Ok" in {
        MockAuthService.authenticateNoProfile returns Future.successful(true)

        val result: Future[Result] = controller.keepAlive(fakeRequest)

        status(result) shouldBe OK
      }

      "no logged in" must {
        "redirect to login" in {
          val loginUrl    = "http://loginHost:1234/sign-in"
          val continueUrl = s"$loginUrl?continue=$requestPath&origin=$appNme"

          MockAppConfig.loginUrl returns loginUrl
          MockAppConfig.loginReturnBase returns returnUrl
          MockAppConfig.appName returns appNme
          MockAuthService.authenticateNoProfile returns Future.successful(false)

          val result: Future[Result] = controller.keepAlive(fakeRequest)
          redirectLocation(result) shouldBe Some(continueUrl)
        }
      }
    }
  }
}
