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
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers.*
import uk.gov.hmrc.icedsubscriptionfrontend.config.MockAppConfig
import uk.gov.hmrc.icedsubscriptionfrontend.services.MockAuthService
import uk.gov.hmrc.icedsubscriptionfrontend.views.html.SuccessfullyEnrolledPage
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class HandbackControllerSpec extends SpecBase with MockAuthService with MockAppConfig {

  val successfullyEnrolledPage: SuccessfullyEnrolledPage = app.injector.instanceOf[SuccessfullyEnrolledPage]

  private val controller =
    new HandbackController(appConfig, stubMessagesControllerComponents(), successfullyEnrolledPage)

  class Test {
  }

  "GET /" should {
    "return 200" in new Test {
      val result: Future[Result] = controller.successfullyEnrolled(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML for the 'Successfully enrolled' page" in new Test {
      val result: Future[Result] = controller.successfullyEnrolled(fakeRequest)
      contentType(result)     shouldBe Some("text/html")
      charset(result)         shouldBe Some("utf-8")
      contentAsString(result) should include("successfullyEnrolled.heading")
    }
  }

}
