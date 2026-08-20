import scoverage.ScoverageKeys

val appName = "iced-subscription-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.7"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    PlayScala,
    SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    PlayKeys.playDefaultPort := 9837,
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.icedsubscriptionfrontend.config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    ),
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*FrontendAuditConnector.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*testonly.*;.*config.*",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    // ***************
    // Use the silencer plugin to suppress warnings
    // You may turn it on for `views` too to suppress warnings from unused imports in compiled twirl templates, but this will hide other warnings.
    scalacOptions ++= Seq(
      "-Wconf:src=html/.*:s",
      "-Wconf:src=target/.*:s",
      "-Wconf:src=.*routes.*:s"
    ),
    scalacOptions ~= (_.distinct)
    // ***************
  )

addCommandAlias("runAllChecks", ";clean;compile;coverage;test;coverageReport")

