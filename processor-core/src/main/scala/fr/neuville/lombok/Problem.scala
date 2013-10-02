package fr.neuville.lombok

import com.intellij.codeInspection.LocalQuickFix
import de.plushnikov.intellij.lombok.problem.ProblemBuilder

sealed trait Problem {
  def message: String
  def fixes: Seq[LocalQuickFix]
}
case class Error(message: String, fixes: LocalQuickFix*) extends Problem
case class Warning(message: String, fixes: LocalQuickFix*) extends Problem

object Problem {
  def addToPbBuilder(pbb: ProblemBuilder)(pb: Problem): Unit =
    pb match {
      case Warning(msg, fixes @ _*) => pbb.addWarning(msg, fixes: _*)
      case Error(msg, fixes @ _*) => pbb.addError(msg, fixes: _*)
    }
}