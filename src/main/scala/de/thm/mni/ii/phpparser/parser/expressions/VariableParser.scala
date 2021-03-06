package de.thm.mni.ii.phpparser.parser.expressions

import fastparse.noApi._
import de.thm.mni.ii.phpparser.parser.literals.WsAPI._

import de.thm.mni.ii.phpparser.ast.{Expressions => EAst}

import de.thm.mni.ii.phpparser.parser.literals.Keywords._
import de.thm.mni.ii.phpparser.parser.literals.KeywordConversions._
import de.thm.mni.ii.phpparser.parser.literals.Literals._

import de.thm.mni.ii.phpparser.parser.Basic._
import de.thm.mni.ii.phpparser.parser.expressions.ExpressionParser.{Expression, ArgumentExpressionList}


/**
  * Created by tobias on 02.06.17.
  */
object VariableParser {

  val VariableName: P[EAst.SimpleNameVar] = P(
    "$" ~ NameWithKeyword).map(EAst.SimpleNameVar)

  val SimpleVariable: P[EAst.SimpleVar] = P(
    ("$" ~ SimpleVariable).map(EAst.SimpleAccessVar)
      | ("$" ~ "{" ~/ Expression ~ "}").map(EAst.SimpleExpVar)
      | VariableName)

  val ArrayElement: P[EAst.ArrayElement] = P(
    ("&" ~ NoCut(Expression)).map(EAst.ArrayElement(None, _, true))
      | (Expression ~ ("=>" ~ "&".!.? ~ Expression).?).map(t =>
      if (t._2.isDefined) EAst.ArrayElement(Some(t._1), t._2.get._2, t._2.get._1.isDefined)
      else EAst.ArrayElement(None, t._1, false))
  )

  val ArrayCreationVar = P(
    ARRAY ~ "(" ~/ !"," ~ ArrayElement.rep(sep = ("," ~ !")").~/) ~ ",".? ~ ")"
      | "[" ~/ !"," ~ ArrayElement.rep(sep = ("," ~ !"]").~/) ~ ",".? ~ "]"
  ).map(EAst.ArrayCreationVar)

  val StringLiteralVar = P(StringLiteral).map(EAst.StringLiteralVar)
  val EnclosedExp = P("(" ~/ Expression ~ ")").map(EAst.EnclosedExp)
  val ScopeAccVar = P((SelfScope | ParentScope | StaticScope) ~~ !NonDigit).map(EAst.ScopeAccessVar)
  val QualifiedNameVar = P(QualifiedName).map(EAst.QualifiedNameVar)

  val Variable: P[EAst.Variable] = {
    val SingleVariable: P[EAst.Variable] = P(
      SimpleVariable | ArrayCreationVar | StringLiteralVar
        | ScopeAccVar | QualifiedNameVar | EnclosedExp)

    val MemberName: P[EAst.MemberName] = P(
      NameWithKeyword.map(EAst.NameMember)
        | SimpleVariable.map(EAst.SimpleVarMember)
        | ("{" ~/ Expression ~ "}").map(EAst.ExpMember))

    P(SingleVariable ~ (
      ("::" ~ MemberName ~ "(" ~/ ArgumentExpressionList ~ ")").map(t => (x: EAst.Variable) => EAst.MemberCallStaticAcc(x, t._1, t._2))
        | ("::" ~ SimpleVariable).map(t => (x: EAst.Variable) => EAst.SimpleVarStaticAcc(x, t))
        | ("(" ~/ ArgumentExpressionList ~ ")").map(t => (x: EAst.Variable) => EAst.CallAccessor(x, t))
        | ("[" ~/ Expression.? ~ "]").map(t => (x: EAst.Variable) => EAst.ArrayAcc(x, t))
        | ("{" ~/ Expression ~ "}").map(t => (x: EAst.Variable) => EAst.BlockAcc(x, t))
        | ("->" ~/ MemberName ~ ("(" ~/ ArgumentExpressionList ~ ")").?).map(t =>
        if (t._2.isDefined) (x: EAst.Variable) => EAst.MemberCallPropertyAcc(x, t._1, t._2.get)
        else (x: EAst.Variable) => EAst.MemberPropertyAcc(x, t._1))
      ).rep
    ).map(t => t._2.foldLeft(t._1)((a, b) => b(a)))
  }
}
