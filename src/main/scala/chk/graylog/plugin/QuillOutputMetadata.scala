package chk.graylog.plugin

import java.net.URI
import java.util

import chk.graylog.plugin.pg.QuillPgOutput
import org.graylog2.plugin.ServerStatus.Capability
import org.graylog2.plugin.{PluginMetaData, Version}

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
class QuillOutputMetadata extends PluginMetaData {

  def getRequiredCapabilities: util.Set[Capability] = util.Collections.emptySet[Capability];

  def getRequiredVersion: Version = new Version(2, 0, 0)

  def getAuthor: String = "chainkite"

  def getName: String = "QuillOutput"

  def getURL: URI = URI.create("http://getquill.io")

  def getDescription: String = "Graylog Output through Quill"

  def getUniqueId: String = classOf[QuillPgOutput].getName

  def getVersion: Version = new Version(1, 1, 0)
}
