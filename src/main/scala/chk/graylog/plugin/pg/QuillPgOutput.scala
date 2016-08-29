package chk.graylog.plugin.pg

import java.nio.charset.Charset
import java.time.Instant
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

import chk.graylog.plugin.parser.InsertAstHelper
import chk.graylog.plugin.{InsertContext, QuillOutputSupport}
import com.google.inject.assistedinject.Assisted
import com.typesafe.{config => typesafe}
import io.getquill.{PostgresAsyncContext, SnakeCase}
import org.graylog2.plugin.Message
import org.graylog2.plugin.configuration.Configuration
import org.graylog2.plugin.outputs.MessageOutput
import org.graylog2.plugin.streams.Stream
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.util.Try

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
class QuillPgOutput(
  private val ctx: PostgresAsyncContext[SnakeCase],
  private val dmlContext: InsertContext) extends MessageOutput {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[QuillPgOutput])

  private final val isClosed = new AtomicBoolean(true)

  def this(ctxAndDmlContext: (PostgresAsyncContext[SnakeCase], InsertContext)) = {
    this(ctxAndDmlContext._1, ctxAndDmlContext._2)
    isClosed.compareAndSet(true, false)
  }

  def this(conf: Configuration) = this(QuillPgOutput.init(conf))

  @Inject
  def this(@Assisted stream: Stream, @Assisted conf: Configuration) = this(QuillPgOutput.init(conf))

  def stop(): Unit = {
    ctx.close()
    isClosed.compareAndSet(false, true)
  }

  def write(message: Message): Unit = {
    val insertSql = dmlContext.insertAst.toInsertSql(buildParamsMap(message))
    ctx.executeAction(insertSql).recover {
      case t: Throwable =>
        logger.error(s"Error bulk writing to postgresql: ${insertSql}", t)
        throw t
    }
  }

  def write(messages: util.List[Message]): Unit = {
    val insertSql = dmlContext.insertAst.toBulkInsertSql(messages.toList.map(buildParamsMap _))
    ctx.executeAction(insertSql).recover {
      case t: Throwable =>
        logger.error(s"Error bulk writing to postgresql: ${insertSql}", t)
        throw t
    }
  }


  private def buildParamsMap(message: Message): Map[String, String] = {
    dmlContext.fields.foldLeft(Map.empty[String, String]) {
      case (paramsMap, field) => paramsMap + (field.fieldNameInLog -> {
        try {
          field.fieldType match {
            case t if t.equalsIgnoreCase("int") =>
              message.getFieldAs[java.lang.Integer](
                classOf[java.lang.Integer], field.fieldNameInLog).toString

            case t if t.equalsIgnoreCase("long") =>
              message.getFieldAs[java.lang.Long](
                classOf[java.lang.Long], field.fieldNameInLog).toString

            case t if t.equalsIgnoreCase("float") =>
              message.getFieldAs[java.lang.Float](
                classOf[java.lang.Float], field.fieldNameInLog).toString

            case t if t.equalsIgnoreCase("double") =>
              message.getFieldAs[java.lang.Double](
                classOf[java.lang.Double], field.fieldNameInLog).toString

            case t if t.equalsIgnoreCase("string") || t.equalsIgnoreCase("java.lang.String") =>
              // convert charset here
              s"""'${dmlContext.charsetConvert(message.getField(field.fieldNameInLog).toString)}'"""

            case "org.joda.time.DateTime" =>
              s"""'${message.getFieldAs[DateTime](classOf[DateTime], field.fieldNameInLog).toString}'"""

            case "java.time.Instant" =>
              s"""'${message.getFieldAs[Instant](classOf[Instant], field.fieldNameInLog).toString}'"""

            case x =>
              s"""'${Class.forName(x).cast(message.getField(field.fieldNameInLog)).toString}'"""

          }
        } catch {
          case t: NullPointerException => "null"

          case t: ClassNotFoundException =>
            throw new IllegalArgumentException(s"Unsupported field type in ${field.toStringInLog}", t)

          case t: Throwable =>
            throw new IllegalArgumentException(
              s"Invalid field in log ${field.toStringInLog}, " +
                s"value: ${message.getField(field.fieldNameInLog)}", t)

        }
      })
    }
  }

  def isRunning: Boolean = !isClosed.get()
}

object QuillPgOutput extends QuillOutputSupport {


  final val DEFAULT_PORT = 3306

  class Descriptor extends MessageOutput.Descriptor(
    "Quill Postgres Output", false, "", "An output sending to Postgres over Quill")


  def init(conf: Configuration): (PostgresAsyncContext[SnakeCase], InsertContext) = {
    val host = Try(conf.getString(CK_HOST))
      .getOrElse(throw new IllegalArgumentException("PostgreSQL host invalid"))

    val port = Try(conf.getInt(CK_PORT)).filter(_ > 0)
      .getOrElse(throw new IllegalArgumentException("PostgreSQL port invalid"))

    val db = Try(conf.getString(CK_DB))
      .getOrElse(throw new IllegalArgumentException("PostgreSQL database name invalid"))

    val username = Try(conf.getString(CK_USERNAME))
      .getOrElse(throw new IllegalArgumentException("PostgreSQL username invalid"))

    val password = Try(conf.getString(CK_PASSWORD))
      .getOrElse(throw new IllegalArgumentException("PostgreSQL password invalid"))

    val dbCharset = Try(Charset.forName(conf.getString(CK_DB_CHARSET))).getOrElse(DEFAULT_DB_CHARSET)

    val poolSize = Try(conf.getInt(CK_POOL_SIZE)).filter(_ >= 0).getOrElse(DEFAULT_POOL_SIZE)
    val bulkInsertSize = Try(conf.getInt(CK_BULK_INSERT_SIZE)).filter(_ > 0).getOrElse(DEFAULT_BULK_INSERT_SIZE)
    val timeout = Try(conf.getInt(CK_TIME_OUT)).filter(_ > 0).getOrElse(DEFAULT_TIME_OUT)

    val dml = Option(conf.getString(CK_DML))
      .getOrElse(throw new IllegalArgumentException("Insert statement is required"))

    val pgConfig = typesafe.ConfigFactory.parseMap(Map(
      "host" -> host,
      "port" -> port,
      "user" -> username,
      "password" -> password,
      "database" -> db,
      "poolMaxObjects" -> poolSize
    ))
    val ctx = new PostgresAsyncContext[SnakeCase](pgConfig)

    val dmlContext = InsertAstHelper.parse(InsertAstHelper.insertClause, dml) match {
      case InsertAstHelper.Success(ast, _) => InsertContext(ast, bulkInsertSize, timeout.seconds)

      case InsertAstHelper.NoSuccess(_, _) =>
        throw new IllegalArgumentException("Cannot parse insert statement")
    }

    (ctx, dmlContext)
  }
}