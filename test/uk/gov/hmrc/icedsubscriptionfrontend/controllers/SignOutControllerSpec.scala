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

package uk.gov.hmrc.icedsubscriptionfrontend.controllers

import base.SpecBase
import play.api.http.Status
import play.api.mvc.{Result, Session}
import play.api.test.Helpers._
import uk.gov.hmrc.icedsubscriptionfrontend.config.MockAppConfig
import uk.gov.hmrc.icedsubscriptionfrontend.views.html.SignedOutPage
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.{Await, Future}

class SignOutControllerSpec extends SpecBase with MockAppConfig {

  val signedOutPage: SignedOutPage = app.injector.instanceOf[SignedOutPage]

  private val controller = new SignOutController(signedOutPage, stubMessagesControllerComponents(), mockAppConfig)

  class Test {
    MockAppConfig.footerLinkItems returns Nil anyNumberOfTimes ()
  }

  "GET /sign-out" when {
    "given a continue URL" should {
      val continueUrl = Some("/continue")

      "return 303" in {
        val result = controller.signOut(continueUrl)(fakeRequest)

        status(result) shouldBe 303
      }

      "redirect to given URL" in {
        val result = controller.signOut(continueUrl)(fakeRequest)

        redirectLocation(result) shouldBe continueUrl
      }

      "start a new session" in {
        val result = controller.signOut(continueUrl)(fakeRequest.withSession(("test", "test")))

        Await.result(result, defaultAwaitTimeout.duration).newSession shouldBe Some(Session())
      }
    }

    "not given a continue URL" should {
      "return 303" in {
        val result = controller.signOut(None)(fakeRequest)

        status(result) shouldBe 303
      }

      "redirect to the signed page" in {
        val result = controller.signOut(None)(fakeRequest)

        redirectLocation(result) shouldBe Some("/safety-and-security-subscription/signed-out")
      }

      "start a new session" in {
        val result = controller.signOut(None)(fakeRequest.withSession(("test", "test")))

        Await.result(result, defaultAwaitTimeout.duration).newSession shouldBe Some(Session())
      }
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
