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

package uk.gov.hmrc.icedsubscriptionfrontend.services

import base.SpecBase
import org.scalamock.handlers.CallHandler
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{Enrolments, _}
import uk.gov.hmrc.icedsubscriptionfrontend.actions.UserType
import uk.gov.hmrc.icedsubscriptionfrontend.connectors.MockAuthConnector

import scala.concurrent.Future

class AuthServiceSpec extends SpecBase with MockAuthConnector {

  val service = new AuthService(mockAuthConnector)

  "AuthService.authenticate" when {
    def stubAuth()
      : CallHandler[Future[Enrolments ~ Option[CredentialRole] ~ Option[AffinityGroup] ~ Option[Credentials]]] =
      MockAuthConnector.authorise(
        AuthProviders(AuthProvider.GovernmentGateway),
        allEnrolments and credentialRole and affinityGroup and credentials)

    def activeEnrolment(key: String): Enrolment = Enrolment(key = key)

    val ssEnrolmentIdentifier = "EoriNumber"
    val ssEnrolmentValue      = "GB1234567890"
    val ssEnrolment = activeEnrolment("HMRC-SS-ORG").copy(
      identifiers = Seq(EnrolmentIdentifier(ssEnrolmentIdentifier, ssEnrolmentValue)))

    val activeSsEnrolments       = Enrolments(Set(ssEnrolment))
    val activeSsEnrolmentsNoEori = Enrolments(Set(activeEnrolment("HMRC-SS-ORG")))

    val otherEnrolments = Enrolments(Set(activeEnrolment("OTHER")))
    val emptyEnrolments = Enrolments(Set.empty)

    val ggwCreds = Credentials(providerId = "someId", providerType = "GovernmentGateway")

    "there is no active session" must {
      "return NotLoggedIn" in {
        stubAuth() returns Future.failed(new NoActiveSession("") {})

        service.authenticate().futureValue shouldBe AuthResult.NotLoggedIn
      }
    }

    "user is an individual (regardless of enrolments)" must {
      Seq(activeSsEnrolments, otherEnrolments, emptyEnrolments).foreach(test)

      def test(enrolments: Enrolments): Unit = s"return UnsupportedAffinityIndividual with enrolments $enrolments" in {
        stubAuth() returns Future.successful(
          enrolments and Some(Assistant) and Some(AffinityGroup.Individual) and Some(ggwCreds))

        service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.UnsupportedAffinityIndividual)
      }
    }

    "user is an agent (regardless of enrolments)" must {
      Seq(activeSsEnrolments, otherEnrolments, emptyEnrolments).foreach(test)

      def test(enrolments: Enrolments): Unit = s"return UnsupportedAffinityAgent with enrolments $enrolments" in {
        stubAuth() returns Future.successful(
          enrolments and Some(Assistant) and Some(AffinityGroup.Agent) and Some(ggwCreds))

        service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.UnsupportedAffinityAgent)
      }
    }

    "user has no affinity group (regardless of enrolments)" must {
      Seq(activeSsEnrolments, otherEnrolments, emptyEnrolments).foreach(test)

      def test(enrolments: Enrolments): Unit = s"return NonGovernmentGatewayUser with enrolments $enrolments" in {
        stubAuth() returns Future.successful(enrolments and Some(Assistant) and None and Some(ggwCreds))

        service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.NonGovernmentGatewayUser)
      }
    }

    "user is a 'Verify' user (regardless of enrolments and affinity group)" must {
      for {
        enrolment <- Seq(activeSsEnrolments, otherEnrolments, emptyEnrolments)
        affinityGroup <- Seq(
                          None,
                          Some(AffinityGroup.Organisation),
                          Some(AffinityGroup.Individual),
                          Some(AffinityGroup.Agent))
      } test(enrolment, affinityGroup)

      def test(enrolments: Enrolments, affinityGroup: Option[AffinityGroup]): Unit =
        s"return UnsupportedVerifyUser with enrolments $enrolments and affinityGroup $affinityGroup" in {
          val verifyCreds = Credentials(providerId = "someVerifyId", providerType = "Verify")
          stubAuth() returns Future.successful(enrolments and Some(Assistant) and affinityGroup and Some(verifyCreds))

          service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.UnsupportedVerifyUser)
        }
    }

    "user is some other non-GGW user" must {
      "return NonGovernmentGatewayUser" in {
        stubAuth() returns Future.failed(UnsupportedAuthProvider())

        service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.NonGovernmentGatewayUser)
      }
    }

    "user has no enrolment" must {
      "return NotEnrolled" in {
        stubAuth() returns Future.successful(
          emptyEnrolments and Some(User) and Some(AffinityGroup.Organisation) and Some(ggwCreds))

        service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.NotEnrolled)
      }
    }

    "user has a different enrolment" must {
      "return NotEnrolled" in {
        stubAuth() returns Future.successful(
          otherEnrolments and Some(User) and Some(AffinityGroup.Organisation) and Some(ggwCreds))

        service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.NotEnrolled)
      }
    }

    "user HMRC-SS-ORG enrolment is not active" must {
      "return NotEnrolled" in {
        stubAuth() returns Future.successful(Enrolments(Set(ssEnrolment.copy(state = "disabled"))) and Some(User) and Some(
          AffinityGroup.Organisation) and Some(ggwCreds))

        service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.NotEnrolled)
      }
    }

    "user HMRC-SS-ORG enrolment is active" must {
      "return AlreadyEnrolled with an EORI" in {
        stubAuth() returns Future.successful(
          activeSsEnrolments and Some(User) and Some(AffinityGroup.Organisation) and Some(ggwCreds))

        service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.AlreadyEnrolled(Some("GB1234567890")))
      }

      "return AlreadyEnrolled without an EORI" in {
        stubAuth() returns Future.successful(
          activeSsEnrolmentsNoEori and Some(User) and Some(AffinityGroup.Organisation) and Some(ggwCreds))

        service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.AlreadyEnrolled(None))
      }
    }

    "user is an Assistant" must {
      "return AlreadyEnrolled with an EORI" when {
        "a HMRC-SS-ORG enrollment is present" in {
          stubAuth() returns Future.successful(
            activeSsEnrolments and Some(Assistant) and Some(AffinityGroup.Organisation) and Some(ggwCreds))

          service.authenticate().futureValue shouldBe AuthResult.LoggedIn(
            UserType.AlreadyEnrolled(Some("GB1234567890")))
        }
      }

      "return AlreadyEnrolled without an EORI" when {
        "a HMRC-SS-ORG enrollment is present" in {
          stubAuth() returns Future.successful(
            activeSsEnrolmentsNoEori and Some(Assistant) and Some(AffinityGroup.Organisation) and Some(ggwCreds))

          service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.AlreadyEnrolled(None))
        }
      }

      "return WrongCredentialRole" when {
        "no HMRC-SS-ORG enrollment is present" in {
          stubAuth() returns Future.successful(
            otherEnrolments and Some(Assistant) and Some(AffinityGroup.Organisation) and Some(ggwCreds))

          service.authenticate().futureValue shouldBe AuthResult.LoggedIn(UserType.WrongCredentialRole)
        }
      }
    }
  }

  "AuthService.authenticateNoProfile" when {
    def stubAuth(): CallHandler[Future[Unit]] = MockAuthConnector.authorise(EmptyPredicate, EmptyRetrieval)

    "logged in" must {
      "return true" in {
        stubAuth() returns Future.unit

        service.authenticateNoProfile().futureValue shouldBe true
      }
    }

    "not logged in" must {
      "return true" in {
        stubAuth() returns Future.failed(new NoActiveSession("") {})

        service.authenticateNoProfile().futureValue shouldBe false
      }
    }
  }
}
