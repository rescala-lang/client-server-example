package viscel

import rescala.default.{Evt, implicitScheduler}
import viscel.server.{JettyServer, ServerPages}
import viscel.shared.JsoniterCodecs

import java.nio.file.{Files, Path}
import java.util.TimerTask
import java.util.concurrent.{SynchronousQueue, ThreadPoolExecutor, TimeUnit}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.control.NonFatal

class Services(
    val staticDir: Path,
    val urlPrefix: String,
    val interface: String,
    val port: Int
) {

  /* ====== main webserver ====== */

  lazy val serverPages = new ServerPages()

  lazy val server: JettyServer =
    new JettyServer(
      terminate = () => terminateEverything(true),
      pages = serverPages,
      staticPath = staticDir,
      urlPrefix = urlPrefix,
    )

  def startServer() = server.start(interface, port)

  /* ====== notifications ====== */

  def terminateEverything(startedServer: Boolean) = {
    new java.util.Timer().schedule(
      new TimerTask {
        override def run(): Unit = {
          if startedServer then
            server.stop()
            println(s"server should have terminated, but somehow does not, not entirely sure why")
        }
      },
      100
    )

  }

}
