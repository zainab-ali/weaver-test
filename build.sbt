import _root_.cats.syntax.all.*
import com.typesafe.tools.mima.core.{ MissingClassProblem, ProblemFilters }
import laika.helium.config.TextLink
import laika.helium.config.ReleaseInfo
import laika.helium.config.HeliumIcon
import laika.helium.config.IconLink
import laika.helium.config.LinkGroup
import laika.helium.config.VersionMenu
import laika.ast.Path.Root
import laika.ast.Image
import laika.config.LinkValidation
import org.typelevel.sbt.site.TypelevelSiteSettings
import sbt.librarymanagement.Configurations.ScalaDocTool
// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.13" // your current series x.y

ThisBuild / startYear  := Some(2019)
ThisBuild / licenses   := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("baccata", "Olivier Mélois"),
  tlGitHubDev("keynmol", "Anton Sviridov"),
  tlGitHubDev("valencik", "Andrew Valencik")
)

ThisBuild / tlCiHeaderCheck := false

// enable the sbt-typelevel-site laika documentation
ThisBuild / tlSitePublishBranch := Some("main")

ThisBuild / tlCiReleaseBranches := List("main")

// use JDK 11
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))

val scala212         = "2.12.21"
val scala213         = "2.13.18"
val scala3           = "3.3.8"
val allScalaVersions = Seq(scala212, scala213, "3.3.8")
ThisBuild / crossScalaVersions := Seq(scala212, scala213, "3.3.8")
ThisBuild / scalaVersion       := scala213 // the default Scala

val Version = new {
  val catsEffect             = "3.7.0"
  val catsLaws               = "2.13.0"
  val discipline             = "1.7.0"
  val fs2                    = "3.13.0"
  val junit                  = "4.13.2"
  val portableReflect        = "1.1.3"
  val scalaJavaTime          = "2.4.0"
  val scalacheck             = "1.19.0"
  val scalajsMacroTask       = "1.1.1"
  val scalajsStubs           = "1.1.0"
  val testInterface          = "1.0"
  val scalacCompatAnnotation = "0.1.4"
  val http4s                 = "0.23.26"
  val munitDiff              = "1.3.4"
  val snapshot4s             = _root_.snapshot4s.BuildInfo.snapshot4sVersion
}

lazy val root = tlCrossRootProject.aggregate(core,
                                             framework,
                                             coreCats,
                                             cats,
                                             scalacheck,
                                             discipline)

def platformSharedSourceSettings: Seq[Setting[_]] = {
  def sharedDirs(config: Configuration) = Def.setting {
    val axes     = virtualAxes.value
    val isJVM    = axes.contains(VirtualAxis.jvm)
    val isJS     = axes.contains(VirtualAxis.js)
    val isNative = axes.contains(VirtualAxis.native)
    val srcMain  = (config / sourceDirectory).value
    val jsNative =
      if (isJS || isNative) Seq(srcMain / "scala-js-native") else Nil
    val jvmNative =
      if (isJVM || isNative) Seq(srcMain / "scala-jvm-native") else Nil
    jsNative ++ jvmNative
  }
  Seq(
    Compile / unmanagedSourceDirectories ++= sharedDirs(Compile).value,
    Test / unmanagedSourceDirectories ++= sharedDirs(Test).value
  )
}

lazy val core = projectMatrix.in(file("modules/core"))
  .jvmPlatform(scalaVersions = allScalaVersions)
  .jsPlatform(scalaVersions = allScalaVersions)
  .nativePlatform(scalaVersions = allScalaVersions)
  .settings(platformSharedSourceSettings)
  .settings(
    name := "weaver-core",
    libraryDependencies ++= Seq(
      "co.fs2"        %%% "fs2-core"    % Version.fs2,
      "org.typelevel" %%% "cats-effect" % Version.catsEffect,
      // https://github.com/portable-scala/portable-scala-reflect/issues/23
      "org.portable-scala"     %%%
        "portable-scala-reflect" % Version.portableReflect cross
        CrossVersion.for3Use2_13,
      "org.typelevel" %% "scalac-compat-annotation" %
        Version.scalacCompatAnnotation,
      "org.scalameta" %%% "munit-diff" % Version.munitDiff
    ) ++
      (if (scalaVersion.value.startsWith("3.")) Nil
       else Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value))
  )

// Shades the munit-diff dependency.
lazy val munitDiffShadingSettings = Seq(
  shadedDependencies += "org.scalameta" %%% "munit-diff" % "<ignored>",
  shadingRules += ShadingRule.moveUnder("munit.diff",
                                        "weaver.internal.shaded"),
  validNamespaces ++= Set("weaver", "org"),
  mimaBinaryIssueFilters += ProblemFilters.exclude[MissingClassProblem](
    "weaver.internal.shaded.*")
)

def addCoreJvmSettings(proj: Project): Project =
  proj.settings(
    libraryDependencies ++= Seq(
      "org.scala-js"  %%%
        "scalajs-stubs" % Version.scalajsStubs % "provided" cross
        CrossVersion.for3Use2_13,
      "junit" % "junit" % Version.junit % Optional
    ),
    munitDiffShadingSettings
  ).enablePlugins(ShadingPlugin)

lazy val coreJVM_212 = addCoreJvmSettings(core.jvm(scala212))
lazy val coreJVM     = addCoreJvmSettings(core.jvm(scala213))
lazy val coreJVM_3   = addCoreJvmSettings(core.jvm(scala3))

def addCoreJsSettings(proj: Project): Project =
  proj.settings(munitDiffShadingSettings).enablePlugins(ShadingPlugin)

lazy val coreJS_212 = addCoreJsSettings(core.js(scala212))
lazy val coreJS     = addCoreJsSettings(core.js(scala213))
lazy val coreJS_3   = addCoreJsSettings(core.js(scala3))

lazy val framework = projectMatrix.in(file("modules/framework"))
  .jvmPlatform(scalaVersions = allScalaVersions)
  .jsPlatform(scalaVersions = allScalaVersions)
  .nativePlatform(scalaVersions = allScalaVersions)
  .dependsOn(core)
  .settings(platformSharedSourceSettings)
  .settings(
    name := "weaver-framework",
    libraryDependencies ++= Seq(
      "junit" % "junit" % Version.junit
    )
  )

def addFrameworkJvmSettings(proj: Project): Project =
  proj
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-sbt"   % "test-interface"     % Version.testInterface,
        "org.scala-js"  %%%
          "scalajs-stubs" % Version.scalajsStubs % "provided" cross
          CrossVersion.for3Use2_13
      )
    )
lazy val frameworkJVM_212 = addFrameworkJvmSettings(framework.jvm(scala212))
lazy val frameworkJVM     = addFrameworkJvmSettings(framework.jvm(scala213))
lazy val frameworkJVM_3   = addFrameworkJvmSettings(framework.jvm(scala3))

def addFrameworkJsSettings(proj: Project): Project =
  proj
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion cross
          CrossVersion.for3Use2_13
      )
    )
lazy val frameworkJS_212 = addFrameworkJsSettings(framework.js(scala212))
lazy val frameworkJS     = addFrameworkJsSettings(framework.js(scala213))
lazy val frameworkJS_3   = addFrameworkJsSettings(framework.js(scala3))

def addFrameworkNativeSettings(proj: Project): Project =
  proj
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-native" %%% "test-interface-sbt-defs" % nativeVersion
      )
    )
lazy val frameworkNative_212 =
  addFrameworkNativeSettings(framework.native(scala212))
lazy val frameworkNative =
  addFrameworkNativeSettings(framework.native(scala213))
lazy val frameworkNative_3 =
  addFrameworkNativeSettings(framework.native(scala3))

lazy val coreCats = (projectMatrix.in(file("modules/core-cats")))
  .jvmPlatform(scalaVersions = allScalaVersions)
  .jsPlatform(scalaVersions = allScalaVersions)
  .nativePlatform(scalaVersions = allScalaVersions)
  .dependsOn(core)
  .settings(platformSharedSourceSettings)
  .settings(
    libraryDependencies ++= Seq(
      "junit" % "junit" % Version.junit % ScalaDocTool
    )
  )
  .settings(name := "weaver-cats-core")

def addCoreCatsJsSettings(proj: Project): Project =
  proj
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scala-js-macrotask-executor" %
          Version.scalajsMacroTask)
    )
lazy val coreCatsJS_212 = addCoreCatsJsSettings(coreCats.js(scala212))
lazy val coreCatsJS     = addCoreCatsJsSettings(coreCats.js(scala213))
lazy val coreCatsJS_3   = addCoreCatsJsSettings(coreCats.js(scala3))

lazy val cats = (projectMatrix.in(file("modules/framework-cats")))
  .jvmPlatform(scalaVersions = allScalaVersions)
  .jsPlatform(scalaVersions = allScalaVersions)
  .nativePlatform(scalaVersions = allScalaVersions)
  .dependsOn(framework, coreCats)
  .settings(platformSharedSourceSettings)
  .settings(
    name           := "weaver-cats",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    // Ensure that the source locations in failure messages are identical on CI
    // as when running locally. See `weaver.internals.SourceLocationUrl`.
    Test / envVars := Map("WEAVER_SOURCE_URL" -> "")
  )

def addCatsJvmSettings(proj: Project): Project =
  proj
    .settings(
      libraryDependencies +=
        "com.siriusxm" %% "snapshot4s-core" % Version.snapshot4s % Test,
      // Required for seting the WEAVER_SOURCE_URL environment variable.
      Test / fork := true
    )
    .enablePlugins(Snapshot4sPlugin)

lazy val catsJVM_212 = addCatsJvmSettings(cats.jvm(scala212))
lazy val catsJVM     = addCatsJvmSettings(cats.jvm(scala213))
lazy val catsJVM_3   = addCatsJvmSettings(cats.jvm(scala3))

lazy val scalacheck = (projectMatrix.in(file("modules/scalacheck")))
  .jvmPlatform(scalaVersions = allScalaVersions)
  .jsPlatform(scalaVersions = allScalaVersions)
  .nativePlatform(scalaVersions = allScalaVersions)
  .dependsOn(core, cats % "test->compile")
  .settings(
    name           := "weaver-scalacheck",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck"          % Version.scalacheck,
      "org.typelevel"  %%% "cats-effect-testkit" % Version.catsEffect % Test)
  )

lazy val discipline = (projectMatrix.in(file("modules/discipline")))
  .jvmPlatform(scalaVersions = allScalaVersions)
  .jsPlatform(scalaVersions = allScalaVersions)
  .nativePlatform(scalaVersions = allScalaVersions)
  .dependsOn(core, cats)
  .settings(platformSharedSourceSettings)
  .settings(
    name           := "weaver-discipline",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "discipline-core" % Version.discipline,
      "org.typelevel" %%% "cats-laws"       % Version.catsLaws % Test
    )
  )

lazy val docsOutput = (projectMatrix.in(file("modules/docs")))
  .jvmPlatform(scalaVersions = allScalaVersions)
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core, framework, coreCats, cats, scalacheck, discipline)
  .settings(
    moduleName := "docs-output",
    name       := "output for documentation",
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "fansi" % "0.4.0"
    )
  )

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    ThisBuild / githubWorkflowAddedJobs ~= {
      // Checkout site assets from LFS
      _.map {
        case job: WorkflowJob if job.name === "Generate Site" =>
          job.withSteps(job.steps.map {
            case step: WorkflowStep.Use
                if step.name === Some("Checkout current branch (full)") =>
              step.updatedParams("lfs", "true")
            case other => other
          })
        case other => other
      }
    })
  .dependsOn(
    core.jvm(scala213),
    framework.jvm(scala213),
    coreCats.jvm(scala213),
    cats.jvm(scala213),
    scalacheck.jvm(scala213),
    discipline.jvm(scala213),
    docsOutput.jvm(scala213)
  )
  .settings(
    moduleName := "weaver-docs",
    name       := "Weaver documentation",
    mdocExtraArguments += "--allowCodeFenceIndented",
    // Source location file paths in md files are not relative to the
    // base directory.  This results in incorrect URLs in the website
    // docs. Work around this by setting the WEAVER_SOURCE_URL to an
    // empty string.
    Compile / fork := true,
    envVars        := Map("WEAVER_SOURCE_URL" -> ""),
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-ember-client" % Version.http4s,
      "org.typelevel" %% "cats-laws"           % Version.catsLaws
    ),
    tlSiteHelium ~=
      (_.site.internalCSS(Root / "assets")
        .site.landingPage(
          logo = Some(Image.internal(Root / "assets/logo.png")),
          title = Some("Weaver"),
          subtitle = Some("A test framework that runs everything in parallel."),
          latestReleases = Seq(
            ReleaseInfo("Upcoming Stable Release", "1.0.0")
          ),
          license = Some("Apache2"),
          titleLinks = Seq(
            VersionMenu.create(unversionedLabel = "Getting Started"),
            LinkGroup.create(
              IconLink.external("https://github.com/typelevel/weaver-test",
                                HeliumIcon.github),
              IconLink.external("https://discord.gg/XF3CXcMzqD",
                                HeliumIcon.chat)
            )
          ),
          documentationLinks = Seq(
            TextLink.internal(Root / "overview/installation.md",
                              "Installation"),
            TextLink.internal(Root / "features/expectations.md",
                              "Expectations (Assertions)")
          )
        )),
    laikaConfig ~= (_.withRawContent)
  )
