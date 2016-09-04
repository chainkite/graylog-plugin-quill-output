package chk.graylog.plugin.test

import java.util.{Date, UUID}

import chk.graylog.plugin.QuillOutputSupport
import chk.graylog.plugin.pg.QuillPgOutput
import io.getquill.{PostgresAsyncContext, SnakeCase}
import org.graylog2.plugin.Message
import org.graylog2.plugin.configuration.Configuration
import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalDateTime}
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers, PrivateMethodTester}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

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
  * 　　　　　┃　　　┃      @author chainkite, 8/28/16
  * 　　　　　┃　　　┃　　+  
  * 　　　　　┃　 　　┗━━━┓ + +
  * 　　　　　┃ 　　　　　　　┣┓
  * 　　　　　┃ 　　　　　　　┏┛
  * 　　　　　┗┓┓┏━┳┓┏┛ + + + +
  * 　　　　　　┃┫┫　┃┫┫
  * 　　　　　　┗┻┛　┗┻┛+ + + +
  */
class QuillPgOutputTest
  extends FunSpec with Matchers with PrivateMethodTester with BeforeAndAfterAll {

  val postgres = embedded.EmbeddedPostgres.startedPg

  override def afterAll() = {
    if (output.isRunning) output.stop()
    postgres.stop()
  }

  val confMap = new java.util.HashMap[String, Object]()
  confMap.put(QuillPgOutput.CK_HOST, embedded.EmbeddedPostgres.config.net().host())
  confMap.put(QuillPgOutput.CK_PORT, int2Integer(embedded.EmbeddedPostgres.config.net().port()))
  confMap.put(QuillPgOutput.CK_DB, embedded.EmbeddedPostgres.config.storage().dbName())
  confMap.put(QuillPgOutput.CK_USERNAME, embedded.EmbeddedPostgres.config.credentials().username())
  confMap.put(QuillPgOutput.CK_PASSWORD, embedded.EmbeddedPostgres.config.credentials().password())
  confMap.put(QuillPgOutput.CK_DB_CHARSET, "latin1")
  confMap.put(QuillPgOutput.CK_POOL_SIZE, int2Integer(200))
  confMap.put(QuillPgOutput.CK_BULK_INSERT_SIZE, int2Integer(100))
  confMap.put(QuillPgOutput.CK_DML,
    "insert into test (datetime, level, message) values ({timestamp: org.joda.time.DateTime}, {level}, {full_message})")
  val conf = new Configuration(confMap)
  val output = new QuillPgOutput(conf)

  val outputCtx = {
    val privateClient = PrivateMethod[PostgresAsyncContext[SnakeCase]]('ctx)
    output.invokePrivate(privateClient())
  }

  import outputCtx._
  implicit val jodaDateTimeDecoder = decoder[DateTime] {
    case localDateTime: LocalDateTime => localDateTime.toDateTime(DateTimeZone.getDefault)
    case localDate: LocalDate => localDate.toDateTimeAtStartOfDay
  }

  def truncate() = outputCtx.executeAction("truncate test;")

  def count(): Future[Long] = outputCtx.run(quote(query[Test]).size)

  def selectAll(): Future[List[Test]] = outputCtx.run(quote(query[Test]))


  final val WAIT_TIME = 10.seconds

  describe("QuillPgOutput") {
    it("should running") {
      output.isRunning should be(true)
    }
    it("should write message") {
      noException should be thrownBy Await.result(truncate(), WAIT_TIME)
      lazy val msg = writeOneMsg(0)
      noException should be thrownBy msg
      Await.result(count(), WAIT_TIME) should equal(1)
      Await.result(selectAll, WAIT_TIME) should contain(msg)
    }
    it("should write chinese message to latin1 postgres") {
      noException should be thrownBy Await.result(truncate(), WAIT_TIME)
      //      val latin1 = java.nio.charset.Charset.forName("latin1")
      val utf8 = java.nio.charset.Charset.forName("utf-8")
      val str = new String("哈雷路亚".getBytes("utf-8"), utf8)

      val msg = Test(message = str)
      noException should be thrownBy output.write(msg.toMessage())
      Await.result(selectAll, WAIT_TIME) should contain(msg)
    }
    it("should write multiple messages" +
      "") {
      noException should be thrownBy Await.result(truncate(), WAIT_TIME)
      val size = 10000
      lazy val msgs = writeMultiMsg(size)
      noException should be thrownBy msgs
      Await.result(count(), WAIT_TIME) should equal(size)
      Await.result(selectAll(), WAIT_TIME) should contain theSameElementsAs (msgs)
    }
    it("should insert null value") {
      noException should be thrownBy Await.result(truncate(), WAIT_TIME)
      val msgMap = new java.util.HashMap[String, Object]()
      msgMap.put(Message.FIELD_ID, UUID.randomUUID().toString)
      noException should be thrownBy output.write(new Message(msgMap))
      val select = outputCtx.executeQuery("select message from test;", extractor = { row =>
        row("message") match {
          case null => None
          case _ => Some("wrong")
        }
      })
      Await.result(select, WAIT_TIME).headOption.flatten should be(empty)
    }
    it("should throw invalid field error") {
      noException should be thrownBy Await.result(truncate(), WAIT_TIME)
      val msgMap = new java.util.HashMap[String, Object]()
      msgMap.put(Message.FIELD_ID, UUID.randomUUID().toString)
      msgMap.put(Message.FIELD_FULL_MESSAGE, s"test-error")
      msgMap.put(Message.FIELD_TIMESTAMP, "error datetime type")
      val ex = the[IllegalArgumentException] thrownBy output.write(new Message(msgMap))
      ex.getMessage should include("Invalid field")
    }
    it("should stop") {
      noException should be thrownBy Await.result(truncate(), WAIT_TIME)
      output.isRunning should be(true)
      noException should be thrownBy output.stop()
      output.isRunning should be(false)
    }
  }



  private def writeOneMsg(i: Int) = {
    val msg = Test(i)
    output.write(msg.toMessage())
    Thread.sleep(1000)
    msg
  }

  private def writeMultiMsg(n: Int) = {
    val msgs = (1 to n).map(Test.apply _)
    output.write(msgs.map(_.toMessage).asJava)
    Thread.sleep(1000)
    msgs.toList
  }

  case class Test(
    datetime: DateTime = new DateTime(org.joda.time.DateTimeZone.getDefault).withMillisOfSecond(0),
    level: Int = 0,
    message: String) {

    def toMessage() = {
      val msgMap = new java.util.HashMap[String, Object]()
      msgMap.put(Message.FIELD_ID, UUID.randomUUID().toString)
      msgMap.put(Message.FIELD_FULL_MESSAGE, message)
      msgMap.put(Message.FIELD_LEVEL, int2Integer(level))
      msgMap.put(Message.FIELD_TIMESTAMP, datetime)
      new Message(msgMap)
    }
  }
  object Test {
    def apply(i: Int): Test = Test(message = s"test-$i")
  }

}
