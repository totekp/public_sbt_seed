package prototype

import java.io.{File, IOException, PrintWriter}
import java.security.MessageDigest
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, LocalTime, ZoneId}
import java.util.UUID

import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, _}
import scala.io.{BufferedSource, Source}
import scala.math.BigDecimal.RoundingMode
import scala.util.Try
import scala.xml.Elem

/**
 * Converts a folder of transcripts (*.srt) from Udacity to
 * readable notes in html, which can be pasted to Google Docs.
 *
 * Ex.
 * runMain UdacityNotes -i "/Users/kefuzhou/Downloads/P3L5 Non-Functional Reqs & Arch Styles Subtitles"
 * runMain UdacityNotes -i "/Users/kefuzhou/Downloads/Software Architecture & Design Subtitles" -cf -r 1
 */
object UdacityNotes {

  case class Data(title: String, body: String, duration: Option[FiniteDuration])

  val isDebug: Boolean = false
  println(s"Debugging is ${isDebug}")
  def debug[T](block: => T): Unit = {
    if (isDebug) block
  }

  def main(args: Array[String]): Unit = {
    val path = getArgumentValue("-i", args)
    val isCourseFolder = args.contains("-cf")
    val rotateCount = Try(getArgumentValue("-r", args)).map(_.toInt).getOrElse(0)

    val lessonFolders = {
      if (isCourseFolder) {
        val dir = new File(path)
        assert(dir.isDirectory, s"Dir should be a directory of lesson directories: ${path}")
        val lessonFolders = dir.listFiles().filter(_.isDirectory).toList
        val result = rotate(rotateCount, sortFiles(lessonFolders.toList))
        result
      } else {
        List(new File(path))
      }
    }

    println(s"Processing ${lessonFolders.size} lessons")

    val folderTitle = new File(path).getName
    val pw = new PrintWriter(new File(s"UdacityNotes_${folderTitle}.html"))
    pw.println(
      s"""<!DOCTYPE html>
        |<html>
        |<head>
        |<title>${folderTitle}</title>
        |</head>
        |<body>
      """.stripMargin)
    pw.println(<h1 class="folderTitleHeader">{s"Folder Title: ${folderTitle}"}</h1>)
    val totalVideoTimeString = {
      val parts = lessonFolders.flatMap(_.listFiles().filterNot(ff => ff.getName.startsWith("."))).map(f => getEndTime(f).map(_.toMillis))
      val numUnknowns = parts.count(_.isEmpty)
      val totalMillis = parts.collect{case Some(endTime) => endTime}.sum.millis
      val localTime = LocalTime.ofNanoOfDay(totalMillis.toNanos)
      val unknownString = {
        if (numUnknowns == 0) ""
        else if (numUnknowns == 1) " + 1 Unknown"
        else s" + ${numUnknowns} Unknowns"
      }
      localTime.toString + unknownString
    }

    pw.println(<div>{"Total Video Time: %s".format(totalVideoTimeString)}</div>)
    val timeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ssa z").withZone(ZoneId.of("America/New_York"))
    val updateTimeString = timeFormatter.format(LocalDateTime.now())
    pw.println(<div>{"Updated: %s".format(updateTimeString)}</div>)

    def surroundText(in: String) = s"\n$in\n"

    val tableOfContents = createTableOfContents(lessonFolders)
    pw.println(tableOfContents)

    lessonFolders.foreach { folder =>
      val sectionDatas = getFilesSorted(folder.getAbsolutePath).toVector.map(parseFile)
        .map { data =>
        data.copy(body = parseSubtitleText(data.body))
      }
      val numUnknowns = sectionDatas.count(_.duration.isEmpty)
      val totalLessonFolderTime = sectionDatas.map(_.duration).collect { case Some(d) => d.toMillis }.sum.millis
      val unknownString = {
        numUnknowns match {
          case 0 => ""
          case 1 => " + 1 Unknown"
          case n => s" + ${n} Unknowns"
        }
      }

      val lessonXml: Elem = {
        <div class="lesson">
          <div class="lessonTitle">
            <h2 class="lessonTitleHeader" id={getLessonId(folder.getName)}>
              <a href={"#%s".format(getTableOfContentsId(folder.getName, ""))}>{surroundText(s"Lesson: ${folder.getName} (${sprintEndTime(totalLessonFolderTime)}${unknownString})")}</a>
            </h2>
          </div>{
          sectionDatas.map { data =>
            <div class="sectionData">
              <h3 class="sectionTitleHeader" id={getSectionId(folder.getName, data.title)}>
                <a href={"#%s".format(getTableOfContentsId(folder.getName, data.title))}>
                  {surroundText(s"Section: ${data.title} ${data.duration.map(a => "(%s)".format(sprintEndTime(a))).getOrElse("")}")}
                </a>
              </h3>
            </div>
            <br/>
            <div>{surroundText(wordWrap(data.body, 120))}</div>
            <br/>
          }}</div>
      }
//      val prettyXml = xmlPrettyPrinter.format(lessonXml)
      pw.println(lessonXml)
    }

    pw.println(
      """
        |<script>
        |  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
        |  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
        |  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        |  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
        |
        |  ga('create', 'UA-65621875-1', 'auto');
        |  ga('send', 'pageview');
        |
        |</script>
      """.stripMargin)
    pw.println("</body></html>")
    pw.close()

    val html: String = useThenCloseFile(new File(s"UdacityNotes_${folderTitle}.html"), _.getLines().mkString("\n"))
    val ids: Map[String, String] = """id\s*=\s*"([A-Za-z0-9_]+)_([A-Za-z0-9_]+)"""".r
      .findAllMatchIn(html)
      .map(_.group(2))
      .toList
      .distinct
      .zipWithIndex.map { case (s, i) =>
        (s, BigInt(i).bigInteger.toString(36))
      }.toMap
    val compressedHtml = ids.foldLeft(html) { case (acc, (existingId, newId)) =>
      acc.replaceAll(existingId, newId)
    }
    val pw2 = new PrintWriter(new File(s"UdacityNotes_${folderTitle}.html"))
    pw2.println(compressedHtml)
    pw2.close()
  }

  private def rotate[A](n: Int, ls: List[A]): List[A] = {
    val nBounded = if (ls.isEmpty) 0 else n % ls.length
    if (nBounded < 0) rotate(nBounded + ls.length, ls)
    else (ls drop nBounded) ::: (ls take nBounded)
  }

  val xmlPrettyPrinter = new scala.xml.PrettyPrinter(120, 2)

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

  private def isRangeLine(line: String) = {
    line.take(8).forall(c => "0123456789:,.".contains(c))
  }

  def getFilesSorted(path: String): List[File] = {
    val file = new File(path)
    if (!file.exists()) {
      throw new IOException(s"File not found at: ${file.getAbsolutePath}")
    }
    val files = file.listFiles()
    sortFiles(files)
  }

  def sortFiles(files: Seq[File]): List[File] = {
    val possibleNumbers = Try(files.map(_.getName.split("-",2).head.filter(c => !c.isWhitespace).toDouble).toVector)
    possibleNumbers match {
      case util.Success(numbers) =>
        files.zip(numbers).sortBy(_._2).map(_._1).toList
      case _ =>
        files.sortBy(_.getName).toList
    }
  }

  def parseFile(file: File): Data = {
    val title = getSectionTitleFromFile(file)
    val body = useThenCloseFile(file, _.getLines().mkString("\n"))
    val duration = getEndTime(file)
    Data(title, body, duration)
  }

  private def useThenCloseFile[A](file: File, fn: BufferedSource => A) = {
    val b = Source.fromFile(file)
    val result = fn(b)
    b.close()
    result
  }

  def getSectionTitleFromFile(file: File): String = {
    val pattern = """\d+ - (.*)\.srt""".r
    val title = pattern.findFirstMatchIn(file.getName).get.group(1).trim
    title
  }

  def wordWrap(text: String, maxLength: Int): String = {
    wordWrap(text.split(" "), maxLength)
  }

  def wordWrap(tokens: Seq[String], maxLength: Int): String = {
    var spaceLeft = maxLength
    val spaceWidth = 1
    val sb = StringBuilder.newBuilder
    tokens.foreach { word  =>
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

  def createUUIDString(): String = {
    UUID.randomUUID().toString.replace("-", "")
  }

  def convertByteArrayToHexString(arrayBytes: Array[Byte]): String = {
    val sb = mutable.StringBuilder.newBuilder
    arrayBytes.indices.foreach { i =>
      sb.append(Integer.toString((arrayBytes(i) & 0xff) + 0x100, 16)
        .substring(1))
    }
    sb.toString()
  }

  def sha1Hash(prefix: String, lessonName: String, sectionName: String): String = {
    val input = s"$lessonName$sectionName"
    val bytes = MessageDigest.getInstance("SHA-1").digest(input.getBytes)
    "%s%s".format(prefix,convertByteArrayToHexString(bytes))
  }

  def getSectionId(lessonName: String, sectionName: String) = {
    sha1Hash("s_", lessonName, sectionName)
  }
  
  def getLessonId(lessonName: String) = {
    sha1Hash("l_", lessonName, "")
  }
  
  def getTableOfContentsId(lessonName: String, sectionName: String) = {
    sha1Hash("t_", lessonName, sectionName)
  }

  def createTableOfContents(lessonFolders: Seq[File]): String = {
    val xml = <div><ul>{
      lessonFolders.map { lessonFolder =>
        val (totalLessonFolderTime, numUnknowns) = {
          val parts = lessonFolder.listFiles().map(f => getEndTime(f).map(_.toMillis))
          val numUnknowns = parts.count(_.isEmpty)
          (parts.collect{case Some(endTime) => endTime}.sum.millis, numUnknowns)
        }
        val unknownString = {
          numUnknowns match {
            case 0 => ""
            case 1 => " + 1 Unknown"
            case n => s" + ${n} Unknowns"
          }
        }

        <div>
          <li>
            <div id={getTableOfContentsId(lessonFolder.getName, "")}>
              <a href={s"#${getLessonId(lessonFolder.getName)}"}>{s"${lessonFolder.getName} (${sprintEndTime(totalLessonFolderTime)}${unknownString})"}</a>
            </div>
          </li>
          <ul>{
            sortFiles(lessonFolder.listFiles().toList).map { sectionFile =>
              val sectionTitle = getSectionTitleFromFile(sectionFile)
              val title = s"$sectionTitle ${getEndTime(sectionFile).map(a => "(%s)".format(sprintEndTime(a))).getOrElse("")}"
              <li>
                <a id={getTableOfContentsId(lessonFolder.getName, getSectionTitleFromFile(sectionFile))}
                   href={s"#${getSectionId(lessonFolder.getName, getSectionTitleFromFile(sectionFile))}"}>{title}</a>
              </li>
            }
          }</ul>
        </div>
      }
    }</ul></div>
//    xmlPrettyPrinter.format(xml)
    xml.toString
  }

  def getEndTime(sectionFile: File): Option[FiniteDuration] = {
    val pattern = """(\d+:\d+:[\d\.,]+)$""".r
    val candidateLinesFromLast: Vector[String] = {
      useThenCloseFile(sectionFile, _.getLines().filter(isRangeLine).toVector.reverse)
    }
    val optTotal: Option[FiniteDuration] = {
      candidateLinesFromLast.view.map(line => pattern.findFirstMatchIn(line).map(_.group(0))).collectFirst {
        case Some(endTimeString) =>
          val Array(hh, mm, ss) = endTimeString.replace(",", ".").split(":").map(_.toDouble)
          val total = hh.hours + mm.minutes + ss.seconds
          total
      }
    }
    optTotal
  }

  def sprintEndTime(duration: FiniteDuration): String = {
    val value = BigDecimal(duration.toMillis / 6e4d).setScale(2, RoundingMode.HALF_EVEN).toString
    s"$value minutes"
  }

}
