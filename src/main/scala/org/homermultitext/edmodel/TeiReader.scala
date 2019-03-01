package org.homermultitext.edmodel

import edu.holycross.shot.mid.validator._
import edu.holycross.shot.xmlutils._

import scala.xml._
import scala.io.Source

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._



/** An implementation of the MidMarkupReader trait for HMT project editions.
*
* @param hmtEditionType Type of edition to generate.
*/
case class TeiReader(hmtEditionType : MidEditionType) extends MidMarkupReader {


  // required by MidMarkupReader
  def editionType: MidEditionType = hmtEditionType


  // required by MidMarkupReader
  def recognizedTypes: Vector[MidEditionType] =  TeiReader.editionTypes


  // required by MidMarkupReader
  def editedNode(cn: CitableNode): CitableNode = {
    hmtEditionType match {
      case HmtNamedEntityEdition => NamedEntityReader.neNode(cn)
      case _ => throw new Exception("Don't yet know how to make an edition of type " + hmtEditionType)
    }
  }
}

/** Object for parsing TEI XML into the HMT project object model of an edition. */
object TeiReader {


  /** Vector of MidEditionTypes that this object can produce.
  */
  def editionTypes:  Vector[MidEditionType] =  Vector(
    HmtNamedEntityEdition,
    HmtDiplomaticEdition,
    HmtScribalNormalizedEdition,
    HmtEditorsNormalizedEdition
  )

  /** URL encode any colon characters in s so that s
  * can be used as the extended citation string of a CtsUrn.
  *
  * @param s String to use as extended citation string of a CtsUrn.
  */
  def ctsSafe(s: String): String = {
    val unsafe = ":"
    val encoded = java.net.URLEncoder.encode(unsafe, "utf-8")
    s.replaceAll(unsafe, encoded)
  }



  def rightMost(s: String, subString: String) : String = {
    s.slice(s.indexOf(subString) + 1, s.size)
  }


  // inclusive
  def leftMost(s: String, subString: String) : String = {
    s.slice(0,s.indexOf(subString) + subString.size + 1)
  }



  /** Create an [[HmtToken]] for a single String.
  *
  * @param tknString The textual reading for this token.
  * @param psg Passage comopnent for this token in tokenized edition.
  * @param textContext Textual context within which we need to index occurrences
  * of tknString.
  * @param settings Contextual values within the document for this token.
  */
  def tokenForString(tknString: String, psg: String, textContext : String, settings: TokenSettings) : HmtToken = {
    val subref = ctsSafe(tknString)
    val subrefIndex =  tknString.r.findAllMatchIn(textContext).length
    val subrefUrn = CtsUrn(settings.contextUrn.toString + "@" + subref + "[" + subrefIndex + "]")

    val version = settings.contextUrn.version + "_lextokens"
    val tokenUrn = settings.contextUrn.addVersion(version).addPassage(psg)

    val lexicalCat = if (punctuation.contains(tknString)) {
      Punctuation
    } else {
      settings.lexicalCategory
    }
    val rdgs = Vector(Reading(tknString, settings.status))

    val hmtToken = HmtToken(
      sourceUrn = subrefUrn,
      editionUrn = tokenUrn,
      readings = rdgs,
      lexicalCategory = lexicalCat,
      lexicalDisambiguation =  Cite2Urn("urn:cite2:hmt:disambig.v1:lexical"),
      alternateReading = None,
      discourse = settings.discourse,
      externalSource = settings.externalSource
    )
    hmtToken
  }


  /** Intimidating regular expression splitting strings by HMT Greek punctuation. */
  val punctuationSplitter = "((?<=[,;:⁑\\.])|(?=[,;:⁑\\.]))"

  /** Extract tokens from a TEI text node's string value.
  *
  * @param str String to tokenize.
  * @param settings State of text at this point.
  */
  def tokensFromText(str: String, settings: TokenSettings) : Vector[HmtToken] = {
    val hmtText = HmtChars.hmtNormalize(str)
    val depunctuate =  hmtText.split(punctuationSplitter)
    val tokenStrings = depunctuate.flatMap(_.split("[ ]+")).filter(_.nonEmpty).toVector


    val accumulated = StringBuilder.newBuilder
    val hmtTokens = for ((tknString,idx) <- tokenStrings.zipWithIndex) yield {
      accumulated.append(tknString)
      val psg = settings.contextUrn.passageComponent + "." + idx
      tokenForString(tknString, psg, accumulated.toString, settings)
    }
    hmtTokens.toVector
  }


/*
  def collectWrappedWordReadings(editorialStatus: EditorialStatus, el: scala.xml.Elem)  = {
    val txt = TextReader.collectText(el)
    el.label match {

      case "unclear" => {

        println("STATUS UNCLEAR FOR " + txt)
        //for (ch <- e.child) {
        //  collectWrappedWordReadings(Unclear,ch)
        //}
      }
      case _ => {
        println("KEEP STATUS FOR " + txt)
      }
    }
  }
  */

  /** Extract tokens from a TEI element.
  *
  * @param el Element to tokenize.
  * @param settings State of text at this point.
  */
  def tokensFromElement(el: scala.xml.Elem, settings: TokenSettings) : Vector[HmtToken] = {
      el.label match {
      // Level 0:  omit
      case "note" =>   Vector.empty[HmtToken] // to be removed from archive
      case "figDesc" =>   Vector.empty[HmtToken] // metadata, don't process
      case "ref" =>   Vector.empty[HmtToken] // metadata, don't process


      // Level 1:  editorial status is innermost markup, so we
      // can directly collect text from here.





      case "add" => {
        /*
        val readingString = el.text.replaceAll(" ", "")
        if (readingString.nonEmpty) {

            val rdgOption = Some(Reading(HmtChars.hmtNormalize(readingString), Multiform))

        } else {
          Vector.empty[HmtToken]
        }*/
        Vector.empty[HmtToken]

        /*
        //  multiform?  Or correction?
        wrappedWordBuffer.clear
        collectWrappedWordReadings(Clear,el)
        val alt = AlternateReading(Multiform,wrappedWordBuffer.toVector)
        wrappedWordBuffer.clear
        val newToken = tokenSettings.copy(alternateReading = Some(alt), readings = wrappedWordBuffer.toVector)
        wrappedWordBuffer.clear
        tokenBuffer += newToken
        */
      }


      // Level 2:  tokenization
      case "num" => {
        val newSettings = settings.addCategory(NumericToken)
        val allTokens = for (ch <- el.child) yield {
          collectTokens(ch, newSettings)
        }
        allTokens.toVector.flatten
      }

      // Hope these are just structural elements:
      case structuralElem: String =>  {
        if (validElements.contains(structuralElem)) {
          val tkns = for (ch <- el.child) yield {
            collectTokens(ch, settings)
          }
          tkns.toVector.flatten
        } else {
          //var errorList = tokenSettings.errors :+  "Invalid element name: " + structuralElem
          val newToken = settings //tokenSettings.copy(errors = errorList)

          val tkns = for (ch <- el.child) yield {
            collectTokens(ch, newToken)
          }
          tkns.toVector.flatten
        }

      }
    }

  }



  /** Extract tokens from an XML node, which can be either an Element
  * or a Text node.
  *
  * @param n Node to tokenize.
  * @param settings State of text at this point.
  */
  def collectTokens(n: xml.Node, settings: TokenSettings): Vector[HmtToken] = {
    n match {
      case t: xml.Text => {
        val sanitized = HmtChars.hmtNormalize(t.text)
        tokensFromText(sanitized, settings)
      }
      case e: xml.Elem => {
        tokensFromElement(e, settings)
      }
    }
  }

}
