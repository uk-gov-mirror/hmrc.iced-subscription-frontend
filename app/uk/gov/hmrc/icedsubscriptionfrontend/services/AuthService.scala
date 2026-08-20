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

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.auth.core.{CredentialRole, _}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.icedsubscriptionfrontend.actions.UserType

import scala.concurrent.{ExecutionContext, Future}

sealed trait AuthResult

object AuthResult {
  case object NotLoggedIn extends AuthResult

  case class LoggedIn(userType: UserType) extends AuthResult
}

@Singleton
class AuthService @Inject()(val authConnector: AuthConnector)(using ec: ExecutionContext)
  extends AuthorisedFunctions {

  private val VerifyProviderType = "Verify"

  private val safetyAndSecurityEnrolmentKey = "HMRC-SS-ORG"

  // Note: the logic is primarily implemented using retrievals rather than lists of
  // predicates so that we can closely control the order of checks rather than
  // relying on any correspondence between predicate order and
  // the exception that is thrown in a particular scenario...
  def authenticate()(using hc: HeaderCarrier): Future[AuthResult] =
    authorised(AuthProviders(AuthProvider.GovernmentGateway))
      .retrieve(allEnrolments and credentialRole and affinityGroup and credentials) { retrievals =>
        import UserType.*

        val userType = retrievals match {
          case _ ~ _ ~ Some(Credentials(_, VerifyProviderType)) => UnsupportedVerifyUser
          case enrolments ~ role ~ Some(AffinityGroup.Organisation) ~ _ =>
            if (hasActiveEnrolment(enrolments)) {
              AlreadyEnrolled(getIdentifier(enrolments))
            } else if (isAdmin(role)) {
              NotEnrolled
            } else {
              WrongCredentialRole
            }
          case _ ~ Some(AffinityGroup.Individual) ~ _ => UnsupportedAffinityIndividual
          case _ ~ Some(AffinityGroup.Agent) ~ _      => UnsupportedAffinityAgent
          case _                                      => NonGovernmentGatewayUser
        }

        Future.successful(AuthResult.LoggedIn(userType))
      }
      .recover {
        case _: NoActiveSession         => AuthResult.NotLoggedIn
        case _: UnsupportedAuthProvider => AuthResult.LoggedIn(UserType.NonGovernmentGatewayUser)
      }

  private def hasActiveEnrolment(enrolments: Enrolments) =
    enrolments.getEnrolment(safetyAndSecurityEnrolmentKey).exists(_.isActivated)

  private def isAdmin(role: Option[CredentialRole]) = role.getOrElse(Assistant) == User

  def authenticateNoProfile()(using hc: HeaderCarrier): Future[Boolean] =
    authConnector
      .authorise(EmptyPredicate, EmptyRetrieval)
      .map(_ => true)
      .recover {
        case _: NoActiveSession => false
      }

  private def getIdentifier(enrolments: Enrolments):Option[String] =
    enrolments.getEnrolment(safetyAndSecurityEnrolmentKey) match {
      case Some(enrolment) => enrolment.getIdentifier("EORINumber") map(_.value)
      case _ => None
    }
}
