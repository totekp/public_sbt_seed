import java.io.{File, IOException, PrintWriter}

import scala.io.Source
import scala.util.Try
import scala.xml.Elem

/**
 * Converts a folder of transcripts (*.srt) from Udacity to
 * readable notes in html, which can be pasted to Google Docs.
 *
 * Ex.
 * runMain UdacityNotes -i "/Users/kefuzhou/Downloads/P3L5 Non-Functional Reqs & Arch Styles Subtitles"
 * runMain UdacityNotes -i "/Users/kefuzhou/Downloads/Software Architecture & Design Subtitles" -cf
 */
object UdacityNotes {

  case class Data(title: String, body: String)

  val isDebug: Boolean = false
  println(s"Debugging is ${isDebug}")
  def debug[T](block: => T): Unit = {
    if (isDebug) block
  }

  def main(args: Array[String]): Unit = {
    val path = getArgumentValue("-i", args)
    val isCourseFolder = args.contains("-cf")

    val lessonFolders = {
      if (isCourseFolder) {
        val dir = new File(path)
        assert(dir.isDirectory, s"Dir should be a directory of lesson directories: ${path}")
        val lessonFolders = dir.listFiles().filter(_.isDirectory)
        val result = sortFiles(lessonFolders.toList)
        result
      } else {
        List(new File(path))
      }
    }

    println(s"Processing ${lessonFolders.size} lessons")

    val folderTitle = new File(path).getName
    val pw = new PrintWriter(new File(s"UdacityNotes_${folderTitle}.html"))
    pw.println("<html><body>")
    pw.println(<h1 class="folderTitleHeader">{s"Folder Title: ${folderTitle}"}</h1>)

    val xmlPrettyPrinter = new scala.xml.PrettyPrinter(120, 2)
    def surroundText(in: String) = s"\n$in\n"
    lessonFolders.foreach { folder =>
      val sectionDatas = getFilesSorted(folder.getAbsolutePath).toVector.map(parseFile)
        .map { data =>
        data.copy(body = parseSubtitleText(data.body))
      }
      val lessonXml: Elem = {
        <div class="lesson">
          <div class="lessonTitle"><h2 class="lessonTitleHeader">{surroundText(s"Lesson Title: ${folder.getName}")}</h2></div>{
          sectionDatas.map { data =>
            <div class="sectionData"><h3 class="sectionTitleHeader">{surroundText(s"Section Title: ${data.title}")}</h3></div>
            <br/>
            <div>{surroundText(wordWrap(data.body, 120))}</div>
            <br/>
          }}</div>
      }
//      val prettyXml = xmlPrettyPrinter.format(lessonXml)
      pw.println(lessonXml)
    }

    pw.println("</body></html>")
    pw.close()
  }



  def getArgumentValue(flag: String, args: Array[String]): String = {
    val i = args.indexWhere(_ == flag)
    if (i == -1) {
      throw new IOException(s"Please provide flag ${flag} [value] in argument")
    } else if (i + 1 >= args.length) {
      throw new IOException(s"Please provide value to flag ${flag}")
    } else {
      args(i + 1)
    }
  }


  def parseSubtitleText(input: String): String = {

    def isNumberLine(line: String) = Try(line.toDouble).isSuccess
    def isRangeLine(line: String) = {
      line.take(8).forall(c => "0123456789:,.".contains(c))
    }

    val lines = Source.fromString(input).getLines().toVector
    val textLines = StringBuilder.newBuilder
    var i = 0
    while (i < lines.size) {
      val line = lines(i).trim
      if (line.isEmpty || isNumberLine(line) || isRangeLine(line) ) {
      } else {
        textLines.append(s"$line ")
      }
      i += 1
    }
    textLines.toString()
  }

  def getFilesSorted(path: String): Seq[File] = {
    val file = new File(path)
    if (!file.exists()) {
      throw new IOException(s"File not found at: ${file.getAbsolutePath}")
    }
    val files = file.listFiles()
    sortFiles(files)
  }

  def sortFiles(files: Seq[File]): Seq[File] = {
    val possibleNumbers = Try(files.map(_.getName.split("-",2).head.filter(c => !c.isWhitespace).toDouble).toVector)
    possibleNumbers match {
      case util.Success(numbers) =>
        files.zip(numbers).sortBy(_._2).map(_._1)
      case _ =>
        files.sortBy(_.getName)
    }
  }

  def parseFile(file: File): Data = {
    val pattern = """\d+ - (.*)\.srt""".r
    val title = pattern.findFirstMatchIn(file.getName).get.group(1).trim
    val body = Source.fromFile(file).getLines().mkString("\n")
    Data(title, body)
  }

  def wordWrap(text: String, maxLength: Int): String = {
    var spaceLeft = maxLength
    val spaceWidth = 1
    val sb = StringBuilder.newBuilder
    text.split(" ").foreach { word  =>
      if (word.length + spaceWidth > spaceLeft) {
        sb.append(s"\n$word ")
        spaceLeft = maxLength - word.length - spaceWidth
      } else {
        sb.append(s"$word ")
        spaceLeft -= (word.length + spaceWidth)
      }
    }
    val out = sb.toString()
    debug {
      assert(Source.fromString(out).getLines().forall(_.length <= maxLength), "word wrap violation")
    }
    out
  }
}
