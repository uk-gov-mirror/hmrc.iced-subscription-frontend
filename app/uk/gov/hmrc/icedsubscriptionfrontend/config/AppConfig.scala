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

package uk.gov.hmrc.icedsubscriptionfrontend.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.annotation.unused

trait AppConfig {
  def appName: String
  def loginUrl: String
  def loginReturnBase: String
  def eoriCommonComponentStartUrl: String
  def sessionTimeoutSeconds: Int
  def sessionCountdownSeconds: Int
  def basGatewaySignOutUrl: String
}

@Singleton
class AppConfigImpl @Inject()(config: Configuration, @unused servicesConfig: ServicesConfig) extends AppConfig {
  lazy val appName: String = config.getOptional[String]("appName").getOrElse("APP NAME NOT SET")

  lazy val loginUrl: String        = config.get[String]("login.url")
  lazy val loginReturnBase: String = config.get[String]("login.return-base")

  private val eoriCommonComponentBaseUri  = config.get[String]("eori-common-component-frontend.base")
  private val eoriCommonComponentStartUri = config.get[String]("eori-common-component-frontend.start")
  lazy val eoriCommonComponentStartUrl    = s"$eoriCommonComponentBaseUri$eoriCommonComponentStartUri"

  lazy val sessionTimeoutSeconds: Int   = config.get[Int]("session.timeoutSeconds")
  lazy val sessionCountdownSeconds: Int = config.get[Int]("session.countdownSeconds")

  lazy val basGatewaySignOutUrl: String = config.get[String]("bas-gateway.sign.out.url")
}
