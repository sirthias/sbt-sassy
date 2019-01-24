package io.bullet.sbt.sass

import java.io.File

import org.scalatest.{FreeSpec, Matchers}
import sbt.io.IO
import sbt.io.syntax._

class SassCompilerSpec extends FreeSpec with Matchers {

  "SassCompiler" - {

    "without imports" in {
      IO.withTemporaryDirectory { dir =>
        val input = new File(getClass.getResource("/well-formed.scss").toURI)
        val result = SassCompiler.compile(input, dir / "sbt-sassy-test.css", Nil)
        val css = IO.read(result.target)

        val normalizedCss = css.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")
        normalizedCss should include(".test{font-size:10px;}")
        normalizedCss should include(".test.hidden{display:none;}")

        result.dependencies.size shouldEqual 1
        result.dependencies.head.name shouldEqual "well-formed.scss"
      }
    }

    "with imports" in {
      IO.withTemporaryDirectory { dir =>
        val input = new File(getClass.getResource("/well-formed-using-import.scss").toURI)
        val result = SassCompiler.compile(input, dir / "sbt-sassy-test.css", Nil)
        val css = IO.read(result.target)

        val normalizedCss = css.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")
        normalizedCss should include(".test-import{font-weight:bold;}")
        normalizedCss should include(".test{font-size:10px;}")
        normalizedCss should include(".test.hidden{display:none;}")

        result.dependencies.size shouldEqual 2
        result.dependencies.map(_.name) should contain("_well-formed-import.scss")
      }
    }

    "broken scss" in {
      IO.withTemporaryDirectory { dir =>
        val input = new File(getClass.getResource("/broken.scss").toURI)
        val error = the [SassCompiler.Error] thrownBy SassCompiler.compile(input, dir / "sbt-sassy-test.css", Nil)
        error.getMessage should include("expected \"{\"")
      }
    }
  }
}
