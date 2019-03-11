package io.bullet.sbt.sass

import java.io.File

import sbt.io.IO
import spray.json.{JsArray, JsString, JsonParser}

import scala.sys.process._

object SassCompiler {

  /**
    * @param sourceMap the source map file
    * @param dependencies All dependencies, i.e. source files that were (transitively) read due to import resolution
    */
  final case class Result(source: File, target: File, sourceMap: File, dependencies: Seq[File]) {
    def filesRead = dependencies.toSet + source
    def filesWritten = Set(target, sourceMap)
  }

  val defaultCommand = if( File.separatorChar == '\\') Seq("cmd", "/c", "sass.bat") else Seq("sass")

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
  def compile(input: File,
              output: File,
              loadPath: Seq[File],
              command: Seq[String] = defaultCommand,
              extraOptions: Seq[String] = Seq.empty): Result = {

    def readDependenciesFrom(encodedRelativeSourceMapUrl: String): Result = {
      val relativeSourceMapUrl = java.net.URLDecoder.decode(encodedRelativeSourceMapUrl, "UTF8")
      val sourceMapFile = new File(output.getParentFile, relativeSourceMapUrl)
      val sourceMapJsonSource = IO.read(sourceMapFile)
      val sourceMapJsonObj = JsonParser(sourceMapJsonSource).asJsObject("Expected sourceMap to contain a JSON object")
      sourceMapJsonObj.fields.get("sources") match {
        case None => fail("Expected sourceMap JSON to have a `sources` member")
        case Some(JsArray(elements)) =>
          Result(
            source = input,
            target = output,
            sourceMap = sourceMapFile,
            dependencies = elements.map {
              case JsString(relativePath) => new File(output.getParentFile, relativePath)
              case _ => fail("Expected `sources` array in sourceMap JSON to only contains JSON Strings")
            }
          )
        case _ => fail("Expected `sources` member of JSON sourceMap to be a JSON array")
      }
    }

    val loadPaths = loadPath.flatMap(path => Seq("-I", path.getAbsolutePath))

    run(command ++
      Seq("--source-map", "--source-map-urls", "relative") ++
      loadPaths ++
      extraOptions
      :+ input.getAbsolutePath :+ output.getAbsolutePath)

    val css = IO.read(output)
    sourceMapUrlRegex.findAllMatchIn(css).toList match {
      case List(m) => readDependenciesFrom(m group 1)
      case x => fail(s"Expected exactly one match of regex [$sourceMapUrlRegex] in produced CSS but got ${x.size} matches")
    }
  }

  private def run(cmd: ProcessBuilder): Unit = {
    val stderr = new java.lang.StringBuilder()
    val proc = cmd.run(ProcessLogger(_ => (), stderr.append(_).append(System.getProperty("line.separator"))))
    if (proc.exitValue() != 0) fail(stderr.toString)
  }

  private def fail(msg: String) = throw new Error(msg.toString)

  final class Error(msg: String) extends RuntimeException(msg)
}