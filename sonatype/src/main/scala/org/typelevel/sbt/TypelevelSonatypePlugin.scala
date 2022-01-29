/*
 * Copyright 2022 Typelevel
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

package org.typelevel.sbt

import sbt._, Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import xerial.sbt.Sonatype, Sonatype.autoImport._
import TypelevelKernelPlugin.mkCommand
import sbt.plugins.IvyPlugin

object TypelevelSonatypePlugin extends AutoPlugin {

  override def requires = MimaPlugin && Sonatype && IvyPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val tlSonatypeUseLegacyHost =
      settingKey[Boolean]("Publish to oss.sonatype.org instead of s01 (default: true)")
  }

  import autoImport._

  override def buildSettings =
    Seq(
      tlSonatypeUseLegacyHost := true,
      autoAPIMappings := true
    ) ++ addCommandAlias(
      "tlRelease",
      mkCommand(
        List(
          "reload",
          "project /",
          "+mimaReportBinaryIssues",
          "+publish",
          "tlSonatypeBundleReleaseIfRelevant"))
    )

  override def projectSettings = Seq(
    publishMavenStyle := true, // we want to do this unconditionally, even if publishing a plugin
    sonatypeProfileName := organization.value,
    publishTo := sonatypePublishToBundle.value,
    commands += sonatypeBundleReleaseIfRelevant,
    sonatypeCredentialHost := {
      if (tlSonatypeUseLegacyHost.value)
        "oss.sonatype.org"
      else
        "s01.oss.sonatype.org"
    },
    apiURL := {
      val javadocio = CrossVersion(
        crossVersion.value,
        scalaVersion.value,
        scalaBinaryVersion.value
      ).map { cross =>
        url(
          s"https://www.javadoc.io/doc/${organization.value}/${cross(moduleName.value)}/${version.value}/")
      }
      apiURL.value.orElse(javadocio)
    }
  )

  private def sonatypeBundleReleaseIfRelevant: Command =
    Command.command("tlSonatypeBundleReleaseIfRelevant") { state =>
      if (state.getSetting(isSnapshot).getOrElse(false))
        state // a snapshot is good-to-go
      else // a non-snapshot releases as a bundle
        Command.process("sonatypeBundleRelease", state)
    }
}
