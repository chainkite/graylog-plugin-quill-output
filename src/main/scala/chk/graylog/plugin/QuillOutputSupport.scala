package chk.graylog.plugin

import java.util

import com.google.common.collect.ImmutableMap
import io.netty.util.CharsetUtil
import org.graylog2.plugin.configuration.ConfigurationRequest
import org.graylog2.plugin.configuration.fields.{ConfigurationField, NumberField, TextField}
import org.graylog2.plugin.outputs.MessageOutput

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
trait QuillOutputSupport {

  final val CK_HOST: String = "host"
  final val CK_PORT: String = "port"
  final val CK_DB: String = "db"
  final val CK_USERNAME: String = "username"
  final val CK_PASSWORD: String = "password"
  final val CK_DML: String = "dml"
  final val CK_DB_CHARSET: String = "dbCharset"
  final val CK_POOL_SIZE: String = "poolSize"
  final val CK_BUFFER_SIZE: String = "bufferSize"
  final val CK_BULK_INSERT_SIZE: String = "bulkInsertSize"
  final val CK_TIME_OUT: String = "timeout"

  def DEFAULT_PORT: Int
  final val DEFAULT_DB_CHARSET = CharsetUtil.UTF_8
  final val DEFAULT_POOL_SIZE = 0
  final val DEFAULT_BUFFER_SIZE = 0
  final val DEFAULT_BULK_INSERT_SIZE = 20
  final val DEFAULT_TIME_OUT = 5

  //  lazy val defaultDbCharset = charset.Charset.forName(DEFAULT_DB_CHARSET)

  class Descriptor extends MessageOutput.Descriptor(
    "Quill Output", false, "", "An output sending to database over Quill")


  class Config extends MessageOutput.Config {

    override def getRequestedConfiguration: ConfigurationRequest = {
      val protocols: util.Map[String, String] = ImmutableMap.of("Finagle", "Finagle")
      val configurationRequest: ConfigurationRequest = new ConfigurationRequest
      configurationRequest.addField(
        new TextField(CK_HOST, "Host", "",
          "This is the hostname of the database",
          ConfigurationField.Optional.NOT_OPTIONAL))

      configurationRequest.addField(
        new NumberField(CK_PORT, "Port", DEFAULT_PORT,
          "This is the port of the database",
          ConfigurationField.Optional.NOT_OPTIONAL, NumberField.Attribute.IS_PORT_NUMBER))

      configurationRequest.addField(
        new TextField(CK_DB, "Database Name", "",
          "This is the name of the database",
          ConfigurationField.Optional.NOT_OPTIONAL))

      configurationRequest.addField(
        new TextField(CK_USERNAME, "Username", "",
          "This is the username of the database",
          ConfigurationField.Optional.NOT_OPTIONAL))

      configurationRequest.addField(
        new TextField(CK_PASSWORD, "Password", "",
          "This is the password of the username",
          ConfigurationField.Optional.NOT_OPTIONAL, TextField.Attribute.IS_PASSWORD))

      configurationRequest.addField(
        new TextField(CK_DML, "Insert SQL", "",
          "This is the insert statement.\n " +
            "e.g. insert into log_table(field1, field2)" +
            " values ({field1_name_in_log}, {field2_name_in_log})",
          ConfigurationField.Optional.NOT_OPTIONAL,
          TextField.Attribute.TEXTAREA))

      configurationRequest.addField(
        new NumberField(CK_BULK_INSERT_SIZE, "Bulk Insert Size", DEFAULT_BULK_INSERT_SIZE,
          "This is the size of bulk insert every time, default 20",
          ConfigurationField.Optional.OPTIONAL, NumberField.Attribute.ONLY_POSITIVE))

      configurationRequest.addField(
        new NumberField(CK_TIME_OUT, "Timeout", DEFAULT_TIME_OUT,
          "This is the time out of database operations (second), default 5 seconds",
          ConfigurationField.Optional.OPTIONAL, NumberField.Attribute.ONLY_POSITIVE))

      configurationRequest.addField(
        new NumberField(CK_POOL_SIZE, "Connection Pool Size", DEFAULT_POOL_SIZE,
          "This is the maximum connection pool size, default and can always be 0.\n" +
            "If > 0, there may be delay to see the latest log in database",
          ConfigurationField.Optional.OPTIONAL))

      configurationRequest.addField(
        new NumberField(CK_BUFFER_SIZE, "Buffer Size", DEFAULT_BUFFER_SIZE,
          "This is the buffer size of the insertion buffering pool, \n" +
            "default and can always be 0.\n" +
            "If > 0, there may be delay to see the latest log in database",
          ConfigurationField.Optional.OPTIONAL))

      configurationRequest.addField(
        new TextField(CK_DB_CHARSET, "Database Charset", DEFAULT_DB_CHARSET.displayName(),
          "This is the charset of target database, only support latin1, utf-8 and binary",
          ConfigurationField.Optional.OPTIONAL))

      configurationRequest
    }
  }
}
