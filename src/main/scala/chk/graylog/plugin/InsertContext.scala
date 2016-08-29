package chk.graylog.plugin

import chk.graylog.plugin.parser.InsertAstHelper

import scala.concurrent.duration.Duration

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
  * 　　　　　┃　　　┃      @author chainkite, 8/22/16
  * 　　　　　┃　　　┃　　+
  * 　　　　　┃　 　　┗━━━┓ + +
  * 　　　　　┃ 　　　　　　　┣┓
  * 　　　　　┃ 　　　　　　　┏┛
  * 　　　　　┗┓┓┏━┳┓┏┛ + + + +
  * 　　　　　　┃┫┫　┃┫┫
  * 　　　　　　┗┻┛　┗┻┛+ + + +
  */
case class InsertContext(
  insertAst: InsertAstHelper.Insert,
  bulkInsertSize: Int,
  timeout: Duration, // seconds
  charsetConvert: String => String = identity  // not using at all currently
) {
  lazy val fields = insertAst.fields.zip(insertAst.values).map {
    case (field, fieldInLog) => InsertField(field, fieldInLog.fieldNameInLog, fieldInLog.fieldClass.getOrElse("java.lang.String"))
  }.toList
}

case class InsertField(fieldName: String, fieldNameInLog: String, fieldType: String) {
  def toStringInLog = s"{${fieldNameInLog}:${fieldType}}"
}