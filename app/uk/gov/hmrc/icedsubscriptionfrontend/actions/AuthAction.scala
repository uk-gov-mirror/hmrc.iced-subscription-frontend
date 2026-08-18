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

import play.api.mvc.Results.Redirect
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.icedsubscriptionfrontend.config.AppConfig
import uk.gov.hmrc.icedsubscriptionfrontend.services.{AuthResult, AuthService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

abstract class AuthAction[P[_]](defaultParser: PlayBodyParsers, appConfig: AppConfig)
    extends ActionBuilder[P, AnyContent]
    with FrontendHeaderCarrierProvider {
  protected def redirectToLogin[A](request: Request[A]): Result =
    Redirect(
      appConfig.loginUrl,
      Map("continue" -> Seq(s"${appConfig.loginReturnBase}${request.uri}"), "origin" -> Seq(appConfig.appName)))

  override def parser: BodyParser[AnyContent] = defaultParser.defaultBodyParser
}

class AuthActionWithProfile @Inject()(defaultParser: PlayBodyParsers, authService: AuthService, appConfig: AppConfig)(
  using val executionContext: ExecutionContext)
    extends AuthAction[AuthenticatedRequest](defaultParser, appConfig) {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    given headerCarrier: HeaderCarrier = hc(request)

    authService.authenticate().flatMap {
      case AuthResult.NotLoggedIn        => Future.successful(redirectToLogin(request))
      case AuthResult.LoggedIn(userType) => block(AuthenticatedRequest(request, userType))
    }
  }
}

class AuthActionNoProfile @Inject()(defaultParser: PlayBodyParsers, authService: AuthService, appConfig: AppConfig)(
  using val executionContext: ExecutionContext)
    extends AuthAction[Request](defaultParser, appConfig) {

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    given headerCarrier: HeaderCarrier = hc(request)

    authService.authenticateNoProfile().flatMap { loggedIn =>
      if (loggedIn) block(request) else Future.successful(redirectToLogin(request))
    }
  }
}
