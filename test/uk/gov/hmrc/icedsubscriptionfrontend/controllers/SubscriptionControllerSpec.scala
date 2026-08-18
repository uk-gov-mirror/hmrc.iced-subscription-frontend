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
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.*
import uk.gov.hmrc.icedsubscriptionfrontend.actions.{AuthActionWithProfile, UserType}
import uk.gov.hmrc.icedsubscriptionfrontend.audit.{AuditEvent, MockAuditHandler}
import uk.gov.hmrc.icedsubscriptionfrontend.config.MockAppConfig
import uk.gov.hmrc.icedsubscriptionfrontend.services.{AuthResult, MockAuthService}
import uk.gov.hmrc.icedsubscriptionfrontend.views.html.*
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class SubscriptionControllerSpec extends SpecBase with MockAuthService with MockAppConfig with MockAuditHandler {
  val appNme    = "iced-subscription-frontend"
  val returnUrl = ""

  val alreadyEnrolledPage: AlreadyEnrolledPage         = app.injector.instanceOf[AlreadyEnrolledPage]
  val nonOrgGgwPage: NonOrgGgwPage                     = app.injector.instanceOf[NonOrgGgwPage]
  val individualPage: BadUserIndividualPage            = app.injector.instanceOf[BadUserIndividualPage]
  val agentPage: BadUserAgentPage                      = app.injector.instanceOf[BadUserAgentPage]
  val verifyUserPage: BadUserVerifyPage                = app.injector.instanceOf[BadUserVerifyPage]
  val wrongCredentialRolePage: WrongCredentialRolePage = app.injector.instanceOf[WrongCredentialRolePage]

  val authAction = new AuthActionWithProfile(stubMessagesControllerComponents().parsers, mockAuthService, mockAppConfig)

  private val controller =
    new SubscriptionController(
      mockAppConfig,
      authAction,
      stubMessagesControllerComponents(),
      alreadyEnrolledPage,
      nonOrgGgwPage,
      individualPage,
      agentPage,
      verifyUserPage,
      wrongCredentialRolePage,
      mockAuditHandler
    )

  class Test {
    MockAppConfig.sessionCountdownSeconds.returns(1).anyNumberOfTimes()
    MockAppConfig.sessionTimeoutSeconds.returns(1).anyNumberOfTimes()
  }

  "GET /start" when {
    "not authenticated" should {
      "redirect to a login page" in new Test {
        val loginUrl    = "http://loginHost:1234/sign-in"
        val continueUrl = s"$loginUrl?continue=$requestPath&origin=$appNme"

        MockAppConfig.loginUrl returns loginUrl
        MockAppConfig.loginReturnBase returns returnUrl
        MockAppConfig.appName returns "iced-subscription-frontend"
        MockAuthService.authenticate returns Future.successful(AuthResult.NotLoggedIn)

        val result: Future[Result] = controller.start(fakeRequest)
        redirectLocation(result) shouldBe Some(continueUrl)
      }
    }

    "authenticated without a HMRC-SS-ORG" should {
      "redirect to the eori common component frontend" in new Test {
        val eoriCommonComponentStartUrl: String = "http://localhost:1234/customs-enrolment-services/gbss/subscribe"
        MockAppConfig.eoriCommonComponentStartUrl returns eoriCommonComponentStartUrl
        MockAuthService.authenticate returns Future.successful(AuthResult.LoggedIn(UserType.NotEnrolled))

        val result: Future[Result] = controller.start(fakeRequest)
        redirectLocation(result) shouldBe Some(eoriCommonComponentStartUrl)
      }
    }

    "authenticated with a HMRC-SS-ORG" should {
      "return HTML for the 'Already enrolled' page" in new Test {
        val eori = "GB1234567890"
        MockAuthService.authenticate returns Future.successful(
          AuthResult.LoggedIn(UserType.AlreadyEnrolled(Some(eori))))

        MockAuditHandler.audit(
          AuditEvent(
            auditType       = "AlreadyEnrolled",
            transactionName = "S&S Already Enrolled",
            detail          = Json.obj("eori" -> eori)
          ))

        val result: Future[Result] = controller.start(fakeRequest)

        contentType(result)     shouldBe Some("text/html")
        charset(result)         shouldBe Some("utf-8")
        contentAsString(result) should include("alreadyEnrolled.heading")
      }
    }

    "authenticated with a HMRC-SS-ORG without an eoriNumber" should {
      "return HTML for the 'Already enrolled' page" in new Test {
        MockAuthService.authenticate returns Future.successful(AuthResult.LoggedIn(UserType.AlreadyEnrolled(None)))

        MockAuditHandler.audit(
          AuditEvent(
            auditType       = "AlreadyEnrolled",
            transactionName = "S&S Already Enrolled",
            detail          = JsObject.empty
          ))

        val result: Future[Result] = controller.start(fakeRequest)

        contentType(result)     shouldBe Some("text/html")
        charset(result)         shouldBe Some("utf-8")
        contentAsString(result) should include("alreadyEnrolled.heading")
      }
    }

    "authenticated with a GGW Organisation account and Credential Role of Assistant" should {
      "return HTML for the `Wrong Credential Role` page" in new Test {
        MockAuthService.authenticate returns Future.successful(AuthResult.LoggedIn(UserType.WrongCredentialRole))
        val result: Future[Result] = controller.start(fakeRequest)

        contentType(result)     shouldBe Some("text/html")
        charset(result)         shouldBe Some("utf-8")
        contentAsString(result) should include("wrongRole.heading")
      }
    }

    "authenticated but not as an organisation with GGW" when {
      "an individual" should {
        "return HTML for the 'badUserIndividualPage' page" in new Test {
          MockAuthService.authenticate returns Future.successful(
            AuthResult.LoggedIn(UserType.UnsupportedAffinityIndividual))
          val result: Future[Result] = controller.start(fakeRequest)

          contentType(result)     shouldBe Some("text/html")
          charset(result)         shouldBe Some("utf-8")
          contentAsString(result) should include("individual.heading")
        }
      }
      "an agent" should {
        "return HTML for the 'badUserAgentPage' page" in new Test {
          MockAuthService.authenticate returns Future.successful(AuthResult.LoggedIn(UserType.UnsupportedAffinityAgent))
          val result: Future[Result] = controller.start(fakeRequest)

          contentType(result)     shouldBe Some("text/html")
          charset(result)         shouldBe Some("utf-8")
          contentAsString(result) should include("agent.heading")
        }
      }

      "a 'verify' user" should {
        "return HTML for the 'badUserVerifyPage' page" in new Test {
          MockAuthService.authenticate returns Future.successful(AuthResult.LoggedIn(UserType.UnsupportedVerifyUser))
          val result: Future[Result] = controller.start(fakeRequest)

          contentType(result)     shouldBe Some("text/html")
          charset(result)         shouldBe Some("utf-8")
          contentAsString(result) should include("verify.heading")
        }
      }

      "None of the above" should {
        "return HTML for the 'nonOrganisationPage' page" in new Test {
          MockAuthService.authenticate returns Future.successful(AuthResult.LoggedIn(UserType.NonGovernmentGatewayUser))
          val result: Future[Result] = controller.start(fakeRequest)

          contentType(result)     shouldBe Some("text/html")
          charset(result)         shouldBe Some("utf-8")
          contentAsString(result) should include("nonOrgGgwPage.heading")
        }
      }
    }
  }
}
