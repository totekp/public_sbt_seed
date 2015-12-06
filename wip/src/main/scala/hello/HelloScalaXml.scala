package hello

import scala.xml.Elem

object HelloScalaXml extends App {

  val xml = {
    <tofu:container>
      <tofu:kk>value is kk</tofu:kk>
      <tofu:ee>value is ee</tofu:ee>
      <tofu:ff>value is ff</tofu:ff>
      <tofu:uu>value is uu</tofu:uu>
      <tofu:zz>value is aa</tofu:zz>
    </tofu:container>
  }

  def sortChildrenAscending(parentXml: Elem, depth: Int = 1): Elem = {
    depth match {
      case 0 =>
        parentXml
      case _ if depth > 0 =>
        val sortedElems = parentXml.child
          .collect { case childXml: Elem =>
            sortChildrenAscending(childXml, depth - 1)
          }
          .sortBy(_.label)
        parentXml.copy(child = sortedElems)
      case _ =>
        throw new Exception(s"Invalid depth: ${depth}")
    }
  }
  println(sortChildrenAscending(xml))
}