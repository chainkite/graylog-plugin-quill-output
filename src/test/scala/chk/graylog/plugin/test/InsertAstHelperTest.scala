package chk.graylog.plugin.test

import chk.graylog.plugin.parser.InsertAstHelper
import org.joda.time.DateTime
import org.scalatest.{FunSpec, Matchers, PrivateMethodTester}

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
  * 　　　　　┃　　　┃      @author chainkite, 9/3/16
  * 　　　　　┃　　　┃　　+  
  * 　　　　　┃　 　　┗━━━┓ + +
  * 　　　　　┃ 　　　　　　　┣┓
  * 　　　　　┃ 　　　　　　　┏┛
  * 　　　　　┗┓┓┏━┳┓┏┛ + + + +
  * 　　　　　　┃┫┫　┃┫┫
  * 　　　　　　┗┻┛　┗┻┛+ + + +
  */
class InsertAstHelperTest extends FunSpec with Matchers with PrivateMethodTester {

  import InsertAstHelper._

  val insertDml = "insert into graylog_mysql_output (datetime, level, message) values ({timestamp:org.joda.time.DateTime}, {level:int}, {message})"

  val valuesInLog = Map("timestamp" -> "'2016-01-01T00:00:00Z'", "level" -> "6", "message" -> "'test'")

  val expected = Insert(
    into = "graylog_mysql_output",
    fields = List("datetime", "level", "message"),
    values = List(
      ValueContext("timestamp", Some("org.joda.time.DateTime")),
      ValueContext("level", Some("int")),
      ValueContext("message", None)))

  val insertIntoClause = {
    val privateClause = PrivateMethod[InsertAstHelper.Parser[String]]('insertIntoClause)
    InsertAstHelper.invokePrivate(privateClause())
  }

  val fieldsClause = {
    val privateClause = PrivateMethod[InsertAstHelper.Parser[List[String]]]('fieldsClause)
    InsertAstHelper.invokePrivate(privateClause())
  }

  val valueContextClause = {
    val privateClause = PrivateMethod[InsertAstHelper.Parser[ValueContext]]('valueContextClause)
    InsertAstHelper.invokePrivate(privateClause())
  }

  val valuesClause = {
    val privateClause = PrivateMethod[InsertAstHelper.Parser[List[ValueContext]]]('valuesClause)
    InsertAstHelper.invokePrivate(privateClause())
  }

  describe("InsertAstHelper") {
    it ("should get the table part from dml") {
      val result = parse(insertIntoClause, insertDml)
      result.successful should be (true)
      result.get should equal (expected.into)
    }

    it ("should get the fields part from dml") {
      val result = parse(fieldsClause, insertDml.substring(insertDml.indexOf("(")))
      result.successful should be (true)
      result.get should contain theSameElementsAs (expected.fields)
    }

    it ("should get the value context from dml values part") {
      val result = parse(valueContextClause, "{#field_name_in_log# : org.this.class$}")
      result.successful should be (true)
      result.get should equal (ValueContext("#field_name_in_log#", Some("org.this.class$")))

      val result2 = parse(valueContextClause, "{#field_name_in_log#}")
      result2.successful should be (true)
      result2.get should equal (ValueContext("#field_name_in_log#", None))
    }

    it ("should get the values part from dml") {
      val result = parse(valuesClause, insertDml.substring(insertDml.indexOf("values")))
      result.successful should be (true)
      result.get should contain theSameElementsAs (expected.values)
    }

    it ("should parse the whole dml") {
      val result = parse(insertClause, insertDml)
      result.successful should be (true)
      result.get should equal (expected)

      result.get.toInsertSql should
        equal("insert into graylog_mysql_output (datetime,level,message) values (?,?,?);")
      result.get.toInsertSql(valuesInLog) should
        equal(s"""insert into graylog_mysql_output (datetime,level,message) values ('${"2016-01-01T00:00:00Z"}',6,'test');""")
      result.get.toBulkInsertSql(3) should
        equal("insert into graylog_mysql_output (datetime,level,message) values (?,?,?),(?,?,?),(?,?,?);")
      result.get.toBulkInsertSql(List(valuesInLog, valuesInLog, valuesInLog)) should
        equal(s"""insert into graylog_mysql_output (datetime,level,message) values ('${"2016-01-01T00:00:00Z"}',6,'test'),('${"2016-01-01T00:00:00Z"}',6,'test'),('${"2016-01-01T00:00:00Z"}',6,'test');""")
    }
  }
}
