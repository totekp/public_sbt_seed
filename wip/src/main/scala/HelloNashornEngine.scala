import java.io.FileReader
import javax.script.{ScriptEngineManager, ScriptEngine}

import org.scalatest.Matchers

object HelloNashornEngine extends App with Matchers {

  val manager: ScriptEngineManager = new ScriptEngineManager(getClass.getClassLoader)
  val engine: ScriptEngine = manager.getEngineByName("nashorn")

  engine.eval(new FileReader(this.getClass.getResource("mathjs_2.3.0/math.js").getFile))

  val sampleIn = "8 + 88 / 2 - 2"

  val replacementMap = (0 until 100).map(i => i / 2)
  val (sampleProcessedOut, _) = {
    """(\d+)""".r.findAllIn(sampleIn).matchData.foldLeft((sampleIn, 0)) {
      case ((acc, prevOffset), m) =>
        val numberString = m.group(0)
        val updatedString = s"(${replacementMap(numberString.toInt).toString})"
        val offset = prevOffset + updatedString.length - numberString.length
        (acc.patch(m.start + prevOffset, updatedString, numberString.length), offset)
    }
  }

  assert(sampleProcessedOut === "(4) + (44) / (1) - (1)")
  assert(engine.eval(s"math.eval('1 + 2 + 3 / 4 * 5')") === 6.75)
  assert(engine.eval(s"math.eval('${sampleProcessedOut}')") === 47d) // Javascript uses floats
}
