package chk.graylog.plugin.parser

import scala.util.parsing.combinator._
import scala.util.parsing.combinator.syntactical._

/**
  * 　　　　┏┓　　　┏┓+ +
  * 　　　┏┛┻━━━┛┻┓ + +
  * 　　　┃　　　　　　　┃
  * 　　　┃　　　━　　　┃ ++ + + +
  * 　　 ████━████ ┃+
  * 　　　┃　　　　　　　┃ +
  * 　　　┃　　　┻　　　┃
  * 　　　┃　　　　　　　┃ + +
  * 　　　┗━┓　　　┏━┛
  * 　　　　　┃　　　┃
  * 　　　　　┃　　　┃ + + + +
  * 　　　　　┃　　　┃　　　 Code is far away from bug with the animal protecting
  * 　　　　　┃　　　┃ + 　　
  * 　　　　　┃　　　┃      @author chainkite, 8/21/16
  * 　　　　　┃　　　┃　　+
  * 　　　　　┃　 　　┗━━━┓ + +
  * 　　　　　┃ 　　　　　　　┣┓
  * 　　　　　┃ 　　　　　　　┏┛
  * 　　　　　┗┓┓┏━┳┓┏┛ + + + +
  * 　　　　　　┃┫┫　┃┫┫
  * 　　　　　　┗┻┛　┗┻┛+ + + +
  */
object InsertAstHelper extends JavaTokenParsers with PackratParsers {

  case class Insert(into: String, fields: List[String], values: List[ValueContext]) {
    require(fields.size == values.size)

    def toInsertSql(): String = s"insert into ${into} (${fields.mkString(",")}) " +
      s"values (${values.map(x => "?").mkString(",")});"

    def toInsertSql(vs: Map[String, String]): String = s"insert into ${into} (${fields.mkString(",")}) " +
      s"values (${values.map(x => vs.getOrElse(x.fieldNameInLog, "null")).mkString(",")});"

    def toBulkInsertSql(n: Int): String  = {
      val aValue = s"(${values.map(x => "?").mkString(",")})"
      s"insert into ${into} (${fields.mkString(",")}) values " +
        (1 to n).map(i => aValue).mkString(",") + ";"
    }

    def toBulkInsertSql(nvs: List[Map[String, String]]): String = {
      def aValue(v: Map[String, String]) = s"(${values.map(x => v.getOrElse(x.fieldNameInLog, "null")).mkString(",")})"
      s"insert into ${into} (${fields.mkString(",")}) values " +
        nvs.map(v => aValue(v)).mkString(",") + ";"
    }
  }
  case class ValueContext(fieldNameInLog: String, fieldClass: Option[String])

  private val tableIdent = """[\w\d_\-/\.]+""".r
  private val fieldIdent = """[\w\d_\-#$%@*&\.]+""".r
  private val classIdent = """[\w\d_\-/#$%\.]+""".r

  private val insertIntoClause: Parser[String] = {
    "insert into" ~> tableIdent ^^ (_.toString)
  }

  private val fieldsClause: Parser[List[String]] = {
    "(" ~> repsep(fieldIdent, ",") <~ ")" ^^ (List(_:_*))
  }

  private val valueContextClause: Parser[ValueContext] = {
    "{" ~> (fieldIdent ~ ((":" ~ classIdent)?)) <~ "}" ^^ {
      case n ~ Some((":" ~ c)) => ValueContext(n, Some(c))
      case n ~ None => ValueContext(n, None)
    }
  }

  private val valuesClause: Parser[List[ValueContext]] = {
    "values" ~ """\s*""".r  ~ "(" ~> repsep(valueContextClause, ",") <~ ")" ^^ (List(_:_*))
  }

  val insertClause: Parser[Insert] = insertIntoClause ~ fieldsClause ~ valuesClause ^^ {
    case into ~ fs ~ vs => Insert(into = into, fields = fs, values = vs)
  }
}