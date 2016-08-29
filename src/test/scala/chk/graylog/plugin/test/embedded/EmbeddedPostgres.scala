package chk.graylog.plugin.test.embedded

import com.typesafe.config.ConfigFactory
import io.getquill.{PostgresAsyncContext, SnakeCase}
import ru.yandex.qatools.embed.postgresql._
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

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
object EmbeddedPostgres {

  val config = PostgresConfig.defaultWithDbName("test", "test", "test")

  lazy val startedPg = {
    val runtime: PostgresStarter[PostgresExecutable, PostgresProcess] = PostgresStarter.getDefaultInstance();
    val process = runtime.prepare(config).start()

    val ctx = new PostgresAsyncContext[SnakeCase](ConfigFactory.parseMap(Map(
      "host" -> config.net().host(),
      "port" -> config.net().port(),
      "user" -> "test",
      "password" -> "test",
      "database" -> config.storage().dbName()
    )))

    Await.result(ctx.executeAction(
      """
        |CREATE TABLE test(
        |    datetime timestamp,
        |    level int,
        |    message text
        |);
      """.stripMargin), Duration.Inf)

    ctx.close()
    process
  }
}
