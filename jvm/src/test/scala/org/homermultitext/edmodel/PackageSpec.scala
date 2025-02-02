package org.homermultitext.edmodel

import org.scalatest._
import scala.xml._

class PackageSpec extends FlatSpec  {

  "The edmodel package object" should "map from source text urn to correct urn or derived edition" in pending

  /*"have a function to collect text from an XML node" in {
   val xml = """<div type="scholion" n="hc_5" xmlns="http://www.tei-c.org/ns/1.0"><div type="lemma"> <p/></div><div type="comment"> <p> <choice> <abbr> ουτ</abbr> <expan> οὕτως</expan></choice> δια τοῦ <rs type="waw"> ο</rs> <q> ζεύγνυον</q> ⁑</p></div></div>"""

   val expected = "ουτ οὕτως δια τοῦ ο ζεύγνυον ⁑"
   val actual = collectText(XML.loadString(xml),"").trim.replaceAll("[ ]+"," ")
   assert (expected == actual)
  }

  it should "have configuration of valid characters for each document+token type" in pending
*/



  it should "have a function normalizing strings to HMT form" in pending
/*
  it should "map a string to a list of code points" in pending {
    val s = "μῆνιν"
    val codepts = codeptList(s)
    assert (codepts.size == 5 )
    val uchars = codepts.map(_.toChar)
    val s2 = uchars.mkString
    assert(s == s2)
  }*/

}
