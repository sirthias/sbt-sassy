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

import sbt.io.IO
import sbt.io.syntax._
import utest._

object SassCompilerTest extends TestSuite {

  val tests = Tests {

    "SassCompiler" - {

      "without imports" - {
        IO.withTemporaryDirectory { dir =>
          val input  = new File(getClass.getResource("/well-formed.scss").toURI)
          val result = SassCompiler.compile(input, dir / "sbt-sassy-test.css", Nil)
          val css    = IO.read(result.target)

          val normalizedCss = css.replaceAll("/\\*.*?\\*/", "").replaceAll("\\s+", "")
          normalizedCss.contains(".test{font-size:10px;}") ==> true
          normalizedCss.contains(".test.hidden{display:none;}") ==> true

          result.dependencies.size ==> 1
          result.dependencies.head.name ==> "well-formed.scss"
        }
      }

      "with imports" - {
        IO.withTemporaryDirectory { dir =>
          val input  = new File(getClass.getResource("/well-formed-using-import.scss").toURI)
          val result = SassCompiler.compile(input, dir / "sbt-sassy-test.css", Nil)
          val css    = IO.read(result.target)

          val normalizedCss = css.replaceAll("/\\*.*?\\*/", "").replaceAll("\\s+", "")
          normalizedCss.contains(".test-import{font-weight:bold;}") ==> true
          normalizedCss.contains(".test{font-size:10px;}") ==> true
          normalizedCss.contains(".test.hidden{display:none;}") ==> true

          result.dependencies.size ==> 2
          result.dependencies.map(_.name).contains("_well-formed-import.scss") ==> true
        }
      }

      "broken scss" - {
        IO.withTemporaryDirectory { dir =>
          val input = new File(getClass.getResource("/broken.scss").toURI)
          intercept[SassCompiler.Error] {
            SassCompiler.compile(input, dir / "sbt-sassy-test.css", Nil)
          }.getMessage.contains("expected \"}\"") ==> true
        }
      }
    }
  }
}
