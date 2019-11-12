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

import java.io.File

import io.bullet.borer.{Dom, Json}
import sbt.io.IO

import scala.sys.process._

object SassCompiler {

  /**
    * @param sourceMap the source map file
    * @param dependencies All dependencies, i.e. source files that were (transitively) read due to import resolution
    */
  final case class Result(source: File, target: File, sourceMap: File, dependencies: Seq[File]) {
    def filesRead    = dependencies.toSet + source
    def filesWritten = Set(target, sourceMap)
  }

  val defaultCommand = if (File.separatorChar == '\\') Seq("cmd", "/c", "sass.bat") else Seq("sass")

  private val sourceMapUrlRegex = "sourceMappingURL=(\\S+)".r

  /**
    * Compiles the given SASS/SCSS input file into a CSS output file.
    * Additional options for the `sass` executable may be passed in via `options`.
    * It returns a list of scss source files that were used. Which may be
    * interesting if includes were used.
    *
    * @param input         The input file containing the SASS/SCSS source
    * @param output        The output file for the generated CSS
    * @param loadPath      The path to resolve imports from
    * @param command       How to call `sass` executable
    * @param extraOptions  Additional CLI options for the `sass` executable.
    */
  def compile(
      input: File,
      output: File,
      loadPath: Seq[File],
      command: Seq[String] = defaultCommand,
      extraOptions: Seq[String] = Seq.empty): Result = {

    def readDependenciesFrom(encodedRelativeSourceMapUrl: String): Result = {
      val relativeSourceMapUrl = java.net.URLDecoder.decode(encodedRelativeSourceMapUrl, "UTF8")
      val sourceMapFile        = new File(output.getParentFile, relativeSourceMapUrl)
      val sourceMapJsonSource  = IO.readBytes(sourceMapFile)
      val sourceMapJson        = Json.decode(sourceMapJsonSource).to[Dom.Element].value
      sourceMapJson match {
        case x: Dom.MapElem.Unsized =>
          x.toMap.get(Dom.StringElem("sources")) match {
            case Some(Dom.ArrayElem.Unsized(elements)) =>
              Result(
                source = input,
                target = output,
                sourceMap = sourceMapFile,
                dependencies = elements.map {
                  case Dom.StringElem(relativePath) => new File(output.getParentFile, relativePath)
                  case _                            => fail("Expected `sources` array in sourceMap JSON to only contains JSON Strings")
                }
              )
            case _ => fail("Expected sourceMap JSON to have a `sources` member that is an Array")
          }
        case _ => fail("Expected sourceMap to contain a JSON object")
      }
    }

    run {
      command ++
      List("--source-map", "--source-map-urls", "relative") ++
      loadPath.flatMap(path => List("-I", path.getAbsolutePath)) ++
      extraOptions ++
      List(input.getAbsolutePath, output.getAbsolutePath)
    }

    val css = IO.read(output)
    sourceMapUrlRegex.findAllMatchIn(css).toList match {
      case List(m) => readDependenciesFrom(m group 1)
      case x =>
        fail(s"Expected exactly one match of regex [$sourceMapUrlRegex] in produced CSS but got ${x.size} matches")
    }
  }

  private def run(cmd: ProcessBuilder): Unit = {
    val stderr = new java.lang.StringBuilder()
    val proc   = cmd.run(ProcessLogger(_ => (), stderr.append(_).append(System.getProperty("line.separator"))))
    if (proc.exitValue() != 0) fail(stderr.toString)
  }

  private def fail(msg: String) = throw new Error(msg.toString)

  final class Error(msg: String) extends RuntimeException(msg)
}
