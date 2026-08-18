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

import base.SpecBase
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers.*
import uk.gov.hmrc.icedsubscriptionfrontend.config.MockAppConfig
import uk.gov.hmrc.icedsubscriptionfrontend.views.html.SignedOutPage
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.net.URLDecoder
import scala.concurrent.Future

class SignOutControllerSpec extends SpecBase with MockAppConfig {

  val signedOutPage: SignedOutPage = app.injector.instanceOf[SignedOutPage]

  private val controller = new SignOutController(signedOutPage, stubMessagesControllerComponents())(using mockAppConfig)

  private def setupMockAppConfig(): Unit = {
    val appName    = "iced-subscription-frontend"
    val returnUrl = "http://localhost:9837"
    val basGatewaySignOutUrl = "http://localhost:9553/bas-gateway/sign-out-without-state"

    MockAppConfig.basGatewaySignOutUrl returns basGatewaySignOutUrl
    MockAppConfig.loginReturnBase returns returnUrl
    MockAppConfig.appName returns appName
  }

  private def decode(url: String): String =
    URLDecoder.decode(url, "UTF-8")

  class Test {
  }

  "GET /sign-out" must {
    "return 303" in {
      setupMockAppConfig()
      val result = controller.signOut(fakeRequest)

      status(result) shouldBe 303
    }

    "redirect to the signed-out page" in {
      setupMockAppConfig()
      val result = controller.signOut(fakeRequest)

      decode(redirectLocation(result).get) shouldBe "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9837/safety-and-security-subscription/signed-out&origin=iced-subscription-frontend"
    }
  }

  "GET /sign-out-to-restart" must {
    "return 303" in {
      setupMockAppConfig()
      val result = controller.signOutToRestart(fakeRequest)

      status(result) shouldBe 303
    }

    "redirect to start" in {
      setupMockAppConfig()
      val result = controller.signOutToRestart(fakeRequest)

      decode(redirectLocation(result).get) shouldBe "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9837/safety-and-security-subscription/start&origin=iced-subscription-frontend"
    }
  }

  "GET /" should {
    "return 200" in new Test {
      val result: Future[Result] = controller.signedOut()(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML for the 'signed out' page" in new Test {
      val result: Future[Result] = controller.signedOut()(fakeRequest)
      contentType(result)     shouldBe Some("text/html")
      charset(result)         shouldBe Some("utf-8")
      contentAsString(result) should include("signedOut.heading")
    }
  }
}
