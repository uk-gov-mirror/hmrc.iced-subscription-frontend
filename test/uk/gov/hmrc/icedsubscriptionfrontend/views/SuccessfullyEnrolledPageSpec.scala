/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.icedsubscriptionfrontend.views

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.icedsubscriptionfrontend.views.html.SuccessfullyEnrolledPage

class SuccessfullyEnrolledPageSpec extends SpecBase {

  lazy val view: SuccessfullyEnrolledPage = inject[SuccessfullyEnrolledPage]

  object Selectors {
    val h1        = "h1"
    val paragraph = "p"
    val listItem  = "li"
    val link      = ".govuk-link"
  }

  lazy val html: Html         = view()(messages, FakeRequest())
  lazy val document: Document = Jsoup.parse(html.toString)
  lazy val content: Element   = document.select("#content").first

  "SuccessfullyEnrolledPage" must {

    "have the correct title" in {
      document.title shouldBe "You have successfully enrolled with S&S GB - Enrol with the Safety and Security service - GOV.UK"
    }

    "have a only one page heading" in {
      document.select(Selectors.h1).text shouldBe "You have successfully enrolled with S&S GB"
      document.select(Selectors.h1).size shouldBe 1
    }

    "have indication of when active" in {
      content
        .select(Selectors.paragraph)
        .first
        .text shouldBe "Your enrolment will be active in 2 hours."
    }

    "have a paragraph explaining S&S" in {
      content
        .select(Selectors.paragraph)
        .get(1)
        .text shouldBe "S&S GB handles digital communications between customs administrators and carriers or their appointed representatives."
    }

    "have a list of what S&S incorporates" in {
      content
        .select(Selectors.paragraph)
        .get(2)
        .text shouldBe "S&S GB incorporates the:"
      content
        .select(Selectors.listItem)
        .first
        .text shouldBe "lodging, handling and processing of an Entry Summary declaration (sometimes called an ENS) " +
        "in advance of the arrival of goods into the UK from outside the UK"
      content
        .select(Selectors.listItem)
        .get(1)
        .text shouldBe "issuing of a Movement Reference Number (MRN)"
    }

    "have a link to make a declaration" in {
      content
        .select(Selectors.paragraph)
        .get(3)
        .text shouldBe "Once your account is active, you can submit an Entry Summary declaration."

      content
        .select(Selectors.link)
        .get(0)
        .attr("href") shouldBe "https://www.gov.uk/guidance/making-an-entry-summary-declaration"
    }
  }
}
