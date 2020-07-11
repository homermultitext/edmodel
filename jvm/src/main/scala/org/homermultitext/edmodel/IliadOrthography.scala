package org.homermultitext.edmodel

import edu.holycross.shot.greek._
import edu.holycross.shot.mid.orthography._
import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citevalidator._
import edu.holycross.shot.scm._
import edu.holycross.shot.dse._

import wvlet.log._
import wvlet.log.LogFormatter.SourceCodeLogFormatter


import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.annotation.tailrec

/** Implementation of the MidOrthography trait for orthography of
* Strings the IliadString class.
*/
@JSExportAll object IliadOrthography  extends MidOrthography with LogSupport  {
  //Logger.setDefaultLogLevel(LogLevel.DEBUG)
  // 5 methods required by MidOrthography
  //
  // 1. required by MidOrthography trait
  /** Label for orthographic system.*/
  def orthography: String = "Orthography of Iliad text in manuscripts of the HMT project"

  // 2. required by MidOrthography trait
  /** Test if cp is a valid code point.
  *
  * @param cp Code point to test.
  */
  def validCP(cp: Int): Boolean = {
    if (cp < 127) {
      validAsciiCP(cp)
    } else {
      validUcodeCP(cp)
    }
  }

  def validAsciiCP(cp: Int): Boolean = {
    val cArray = Character.toChars(cp)
    alphabetString.contains(cArray(0))
  }

  def validUcodeCP(cp: Int) : Boolean = {
    allowedInAscii.contains(cp) ||     CodePointTranscoder.transcodableUcodeCP(cp)
  }



  // 3. required by MidOrthography trait
  /** Complete enumeration of MidTokenCategory values. */
  def tokenCategories : Vector[MidTokenCategory] = Vector(
    PunctuationToken, LexicalToken, NumericToken
  )

  // 4. required by MidOrthography trait
  /** Tokenize a citable node.
  *
  * @param n Node to tokenize.
  */
  def tokenizeNode(n: CitableNode): Vector[MidToken] = {
    val urn = n.urn
    // initial chunking on white space
    val lgs = IliadString(n.text)
    val units = lgs.ascii.split("[\\s]+").filter(_.nonEmpty).toVector

    val classified = for ( (unit, idx) <- units.zipWithIndex) yield {
      val newPassage = urn.passageComponent + "." + idx
      val newVersion = urn.addVersion(urn.versionOption.getOrElse("") + IliadOrthography.exemplarId)
      val newUrn = CtsUrn(newVersion.dropPassage.toString + newPassage)

      debug(s"Look at unit ${idx}: " + unit)
      val trimmed = unit.trim

      if (trimmed.isEmpty) {
        n.text.size match {
          case 0 => {
            warn("IliadString: empty text citable node " + n)
            Vector.empty[MidToken]
          }
          case _ => {
            warn (s"IliadString: evil unicode detected.  Text node of length ${n.text.size} has no tokens.")
            warn("Code points were:")
            warn(CodePointTranscoder.sideBySide(n.text).mkString("\n"))
            Vector.empty[MidToken]
          }
        }
        Vector.empty[MidToken]

      } else {
        // Catch leading quotation?
        val tokensClassified: Vector[MidToken] = if (trimmed(0) == '"') {
            Vector(MidToken(newUrn, "\"", Some(PunctuationToken)))

        } else {
          val depunctuated = depunctuate(unit)
          val first =  MidToken(newUrn, depunctuated.head, lexicalCategory(depunctuated.head))

          val trailingPunct = for (punct <- depunctuated.tail zipWithIndex) yield {
            MidToken(CtsUrn(newUrn + "_" + punct._2), punct._1, Some(PunctuationToken))
          }
          first +: trailingPunct
        }
        tokensClassified
      }
    }
    classified.toVector.flatten
  }
  // 5. required by MidOrthography trait
  /** String value to appendin in forming exemplar ID values.*/
  def exemplarId: String = "_lgstkn"


  /** All Unicode code points contained in the  alphabet.*/
  def cpList = CodePointTranscoder.strToCps(alphabetString)





  /** Recursively strips punctuation tokens off the end of a String,
  * to build list of tokens.
  *
  * @param s String to depunctuate.
  * @param depunctVector List of result tokens.
  * @param punctuation String containing all punctuation characters.
  */
  def depunctuate (s: String, depunctVector: Vector[String] = Vector.empty, punctuationChars: String = punctuationString): Vector[String] = {
    val trimmed = s.trim
    val trailChar = s"${trimmed.last}"
    if (punctuationChars.contains(trailChar)) {
      val dropLast = trimmed.reverse.tail.reverse
      if (dropLast.nonEmpty) {
        depunctuate(dropLast, trailChar +: depunctVector)
      } else {
        s +: depunctVector
      }

    } else {
      s +: depunctVector
    }
  }

  /** Identify token type of a string.  Numeric tokens are flagged by
  * the trailing numeric tick character.  Punctuation tokens are those
  * found in an list that defaults to the punctuationString.  Other valid
  * strings are lexical tokens.
  *
  * @param s String to classify.
  * @param punctuationChars List of punctuation characters.
  */
  def lexicalCategory(s: String, punctuationChars: String = punctuationString): Option[MidTokenCategory] = {
    if (s.last == numericTick) {
      Some(NumericToken)

    } else if (punctuationChars.contains(s)) {
      Some(PunctuationToken)

    } else if (IliadString(s).valid) {
      Some(LexicalToken)

    } else {
      None
    }
  }





  ///////////// ORTHOGRAPHIC DEFINITION
  //
  /** All valid characters in the ASCII representation of this system
  * in their alphabetic order in Greek.
  */
  val alphabetString = "*abgdezhqiklmncoprstufxyw'.|()/\\=+^_,~;.⁑ \n\r"
  val passThrough = "⁑~"

  /** Alphabetically ordered Vector of vowel characters in `ascii` view.*/
  val vowels = Vector('a','e','h','i','o','u','w')
  /** Alphabetically ordered Vector of consonant characters in `ascii` view.*/
  val consonants = Vector('b','g','d','z','q','k','l','m','n','c','p',
    'r','s','t','f','x','y')
  /** Breathing characters.*/
  val breathings = Vector(')', '(')
  /** Accent characters.*/
  val accents = Vector('=', '/', '\\')
  /** Characters in addition to breathings and accents that combine with
  * other characters in `ucode` view.*/
  val comboChars = Vector('|','+', '^', '_')

  val combining = breathings ++ accents ++ comboChars

  val punctuationString: String = """,;~.⁑"""
    //"(),;:.?"

  val whiteSpace = Vector(' ','\t', '\n', '\r' )

  val numericTick: Character = 'ʹ'
  val typography = Vector('\'',  '*', numericTick)

  val validList = vowels.mkString("") + consonants.mkString("") + breathings.mkString("") + accents.mkString("") + comboChars.mkString("") + punctuationString.mkString("") + whiteSpace.mkString("") + typography.mkString("")


  val allowedInAscii: Vector[Int] = CodePointTranscoder.strToCps("~⁑")



  /** True if given character is a vowel.
  *
  * @param c Character to check.
  */
  def isVowel (c: Character): Boolean = {vowels.contains(c)}

  /** True if given character is a consonant.
  *
  * @param c Character to check.
  */
  def isConsonant (c: Character): Boolean = {consonants.contains(c)}

  /** True if given character is alphabetic.
  *
  * @param c Character to check.
  */
  def isAlpha(c: Character): Boolean = (isVowel(c) || isConsonant(c))

  /** True if given character is an accent.
  *
  * @param c Character to check.
  */
  def isAccent(c: Character): Boolean = accents.contains(c)


  /** True if given character is a breathing.
  *
  * @param c Character to check.
  */
  def isBreathing(c: Character): Boolean = breathings.contains(c)

  /** True if given character combines with other characters in `ucode` view.
  *
  * @param c Character to check.
  */
  def isCombining(c: Character): Boolean = {
    (comboChars.contains(c) || isAccent(c) || isBreathing(c))
  }

  /** String label for class of a character.
  *
  * @param c Character to classify.
  */
  def classOfChar(c: Character): String = {
    if (vowels.contains(c)) {
      "vowel"
    } else if (consonants.contains(c)) {
      "consonant"
    } else if (breathings.contains(c)) {
      "breathing"
    } else if (accents.contains(c)) {
      "accent"
    } else if (comboChars.contains(c)) {
      "combining"
    } else {
      "invalid"
    }
  }


  /** Use the [[CodePointTranscoder]] object to recursively
  * convert code points represented in `ascii` view to
  * `ucode` code points.
  *
  * @param ascii String to convert to `ucode` view.
  * @param ucode Accumluated string of Unicode code  points
  * in `ucode` view's encoding.

  @tailrec def asciiToUcode(ascii: String, ucode: String): String = {
    //Logger.setDefaultLogLevel(LogLevel.INFO)
    //debug("asciiToUcode: a vs u " + ascii + " vs " + ucode)
    if (ascii.size == 0 ) {
      ucode

    } else if (ascii.size == 1) {
      ucode + CodePointTranscoder.ucodeCodePoint(ascii)

    } else {
      val chunk = peekAhead(ascii,"")
      val newUcode = ucode + CodePointTranscoder.ucodeCodePoint(chunk)
      val newAscii = ascii.drop(chunk.size)
      asciiToUcode(newAscii, newUcode)
    }
  }
  */
  /** Recursively converts code points in a Unicode string in form NFC to
  * equivalent characters in `ascii` view.
  *
  * @param ucode String to convert.  Note that the String must be in
  * Unicode Form NFC.
  * @param ascii String of `ascii` view accumulated so far.

  def nfcToAscii(ucode: String, ascii: String): String = {
    //debug("nfcToAscii: " + ucode + " and " + ascii)
    if (ucode.size == 0 ) {
      debug("going with provided ascii " + ascii)
      ascii

    } else if (ucode.size == 1) {
      debug("transcoding ucode " + ucode)
      ascii +  CodePointTranscoder.asciiCodePoint(ucode)

    } else {
      val newUcode = ucode.drop(1)
      val newAscii = ascii + CodePointTranscoder.asciiCodePoint(ucode.head.toString)
      nfcToAscii(newUcode,newAscii )
    }
  }
  */


}
