package org.homermultitext.edmodel


import scala.collection.mutable.ArrayBuffer
import scala.xml._

object TeiReader {
  // perhaps should be a function retrieving
  // list by text group and lexical category?
  val punctuation = Vector(",",".",";","⁑")

  val validElements = Vector(
    "div", "l","p", "choice",
    "num",
    "unclear","add","orig","reg","sic","corr",
    "abbr","expan",
    "cite","q","ref",
    "persName","placeName",
    "rs"
  )

  var tokenBuffer = scala.collection.mutable.ArrayBuffer.empty[HmtToken]

  var wrappedWordBuffer = scala.collection.mutable.ArrayBuffer.empty[Reading]

  def collectWrappedWordStrings(editorialStatus: EditorialStatus, n: xml.Node): Unit = {
    n match {
      case t: xml.Text => {
        val readingString = t.text.replaceAll(" ", "")
        if (! readingString.isEmpty) {
          wrappedWordBuffer += Reading(readingString  , editorialStatus)
        }
      }

      case e: xml.Elem => {
        e.label match {
          case "unclear" => {
            for (ch <- e.child) {
              collectWrappedWordStrings(Unclear,ch)
            }
          }
          case _ => {
            for (ch <- e.child) {
              collectWrappedWordStrings(editorialStatus,ch)
            }
          }
        }
      }
    }
  }



  def collectTokens(currToken: HmtToken, n: xml.Node): Unit = {
    n match {
      case t: xml.Text => {
        // the awesomeness of regex: split on set of
        // characters without losing them:
        val depunctuate =   t.text.split("((?<=[,;⁑\\.])|(?=[,;⁑\\.]))")
        val tokenList = depunctuate.flatMap(_.split("[ ]+")).filterNot(_.isEmpty)
        for (tk <- tokenList) {
          val rdg = Reading(tk, Clear)
          var newToken = currToken.copy(readings = Vector(rdg))
          if (punctuation.contains(tk)) {
            newToken.lexicalCategory = Punctuation
          }
          tokenBuffer += newToken
        }
      }
      case e: xml.Elem => {
        e.label match {
          case "num" => {
            val newToken = currToken.copy(lexicalCategory = NumericToken)
            for (ch <- e.child) {
              collectTokens(newToken, ch)
            }
          }
          case "w" => {
            wrappedWordBuffer.clear
            collectWrappedWordStrings(Clear,e)
            val newToken = currToken.copy(readings = wrappedWordBuffer.toVector)
            tokenBuffer += newToken
          }
          case "foreign" => {
            val langAttributes = e.attributes.toVector.filter(_.key == "lang").map(_.value)
            require (langAttributes.size == 1)
            val langVal = langAttributes(0).text
            val newToken = currToken.copy(lang = langVal)
            for (ch <- e.child) {
              collectTokens(newToken, ch)
            }
          }
/*

          case "persName" => {

          }
          case "placeName" => {

          }*/
          case l: String =>  {
            if (validElements.contains(l)) {
              for (ch <- e.child) {
                collectTokens(currToken, ch)
              }
            } else {
              var errorList = currToken.errors :+  "Invalid element name: " + l
              val newToken = currToken.copy(errors = errorList)
              for (ch <- e.child) {
                collectTokens(newToken, ch)
              }
            }

          }
        }
      }
    }
  }


  def teiToTokens(urnStr: String, xmlStr: String) : Vector[ (String, HmtToken)]  = {
    val root  = XML.loadString(xmlStr)
    val currToken = HmtToken(
      urn = urnStr,
      lexicalCategory = LexicalToken,
      readings = Vector.empty
    )
    tokenBuffer.clear
    collectTokens(currToken, root)

    // in the final result, add exemplar-level
    // citation element
    val zippedVal = tokenBuffer.zipWithIndex.map{ case (t,i) => {
      val baseUrn = t.urn
      t.urn = baseUrn + "." + (i +1)
      (baseUrn, t) }
    }.toVector


    zippedVal

  }
}
