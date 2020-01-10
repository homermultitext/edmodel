package org.homermultitext.edmodel

import scala.xml._


import wvlet.log._
import wvlet.log.LogFormatter.SourceCodeLogFormatter



object HmtTeiChoice extends LogSupport {
  // Allowed combinations for TEI choice:
  val abbrExpan = Set("abbr","expan")
  val sicCorr = Set("sic", "corr")
  val origReg = Set("orig", "reg")

  val choicePairs = Vector(abbrExpan, sicCorr, origReg)

  def choiceChildren(choiceElem: scala.xml.Elem): Vector[String] = {
    choiceElem.child.map(_.label).distinct.filterNot(_ == "#PCDATA").toVector
  }

  // check structure of TEI choice
  def validChoice(choiceElem: scala.xml.Elem): Boolean = {
    val cNames = choiceChildren(choiceElem).toSet
    if (choicePairs.filter(_ == cNames ).nonEmpty) {
      true
    } else {
      false
    }
  }

  def pairedToken(t1: Vector[HmtToken], t2: Vector[HmtToken], settings: TokenSettings): Vector[HmtToken] = {
    // we want to unify readings and alt readings
    val unified = if (t1.size >= t2.size)  {
      for ((ch,i) <- t1.zipWithIndex) yield {
        ch.alternateReading match {
          case None => {
            val rdgs = ch.readings
            t2(i).addReading(rdgs)
          }
          case _ => {
            //i'm looking at ALT READING
            val alt = ch.alternateReading
            t2(i).addAlternateReading(alt)
          }
        }
      }
    } else {
      for ((ch,i) <- t2.zipWithIndex) yield {
        ch.alternateReading match {
          case None => {
            val rdgs = ch.readings
            t1(i).addReading(rdgs)
          }
          case _ => {
            val alt = ch.alternateReading
            t1(i).addAlternateReading(alt)
          }
        }
      }
    }
    unified
  }

}
