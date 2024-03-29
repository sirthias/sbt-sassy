/*
 * Copyright 2015-2019 Jens Grassel, Mathias Doenitz
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

package io.bullet.sbt.sass

import com.typesafe.sbt.web.{incremental, SbtWeb}
import com.typesafe.sbt.web.incremental._
import com.typesafe.sbt.web.incremental.OpSuccess
import sbt.Keys._
import sbt._

import scala.util.Try

object SbtSass extends AutoPlugin {

  object autoImport {
    val sass = TaskKey[Seq[File]]("sass", "Generate css files from scss and sass.")

    val sassExecutable = SettingKey[Seq[String]](
      "sassExecutable",
      "The full path to the sass executable can be provided here if neccessary.")
    val sassLoadPath = SettingKey[Seq[File]]("sassLoadPath", "Path to use when resolving imports.")

    val sassOptions =
      SettingKey[Seq[String]]("sassOptions", "Additional options that are passed to the sass executable.")
  }

  override def requires: Plugins      = SbtWeb
  override def trigger: PluginTrigger = AllRequirements

  import SbtWeb.autoImport._
  import autoImport._

  val settings =
    inConfig(Assets) {
      inTask(sass) {
        Seq(
          excludeFilter := HiddenFileFilter || "_*",
          includeFilter := "*.sass" || "*.scss",
          resourceManaged := WebKeys.webTarget.value / "sass" / "main",
        )
      } ++ Seq(
        resourceGenerators += sass in Assets,
        managedResourceDirectories += (resourceManaged in sass in Assets).value,
        sassExecutable := SassCompiler.defaultCommand,
        sassLoadPath := (WebKeys.webModuleDirectories in Assets).value,
        sassOptions := Nil,
        sass := Def
          .task {
            val sourceDir = (sourceDirectory in Assets).value
            val targetDir = (resourceManaged in sass in Assets).value
            val loadPath  = (sassLoadPath in Assets).value
            val command   = (sassExecutable in Assets).value
            val options   = (sassOptions in Assets).value
            val included  = (includeFilter in sass in Assets).value
            val excluded  = (excludeFilter in sass in Assets).value
            val cacheDir  = (streams in Assets).value.cacheDirectory
            val log       = streams.value.log

            val sources: Seq[File] = (sourceDir ** (included -- excluded)).get
            IO.createDirectory(targetDir)

            val (allCssFiles, newlyWrittenCssFiles) =
              incremental.syncIncremental(cacheDir / "run", sources) {
                case Nil => Map.empty[File, OpResult] -> Nil
                case modifiedSources =>
                  log.info(s"Sass compiling ${modifiedSources.size} source(s) to $targetDir ...")
                  log.debug(modifiedSources.mkString("  ", "\n  ", ""))

                  val compilationResults = modifiedSources.map { source =>
                    val relSourcePath = IO.relativize(sourceDir, source).get
                    val target        = new File(targetDir, relSourcePath.substring(0, relSourcePath lastIndexOf '.') + ".css")
                    Try(SassCompiler.compile(source, target, loadPath, command, options)).toEither.left.map { e =>
                      log.error(s"Error during Sass compilation of [$source]: $e")
                      source
                    }
                  }
                  val cssFiles = compilationResults.collect { case Right(result) => result.target }
                  val cacheMap = compilationResults.foldLeft(Map.empty[File, OpResult]) {
                    case (m, Right(x))     => m.updated(x.source, OpSuccess(x.filesRead, x.filesWritten))
                    case (m, Left(source)) => m.updated(source, OpFailure)
                  }
                  cacheMap -> cssFiles
              }

            if (newlyWrittenCssFiles.nonEmpty)
              log.debug(s"Sass compilation results:\n  ${newlyWrittenCssFiles.mkString("\n  ")}")

            allCssFiles.toSeq

          }
          .dependsOn(WebKeys.webModules in Assets)
          .value,
      )
    }

  override def projectSettings: Seq[Setting[_]] = settings
}
