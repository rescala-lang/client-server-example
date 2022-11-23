package viscel.server

import better.files.File
import jakarta.servlet.http.{Cookie, HttpServletRequest, HttpServletResponse}
import loci.communicator.ws.jetty.*
import loci.communicator.ws.jetty.WS.Properties
import loci.registry.Registry
import org.eclipse.jetty.http.{HttpCookie, HttpHeader, HttpMethod}
import org.eclipse.jetty.rewrite.handler.{RewriteHandler, RewriteRegexRule}
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.server.handler.{AbstractHandler, HandlerList, HandlerWrapper, ResourceHandler}
import org.eclipse.jetty.server.{Request, Server, ServerConnector}
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.thread.QueuedThreadPool
import rescala.default.Signal
import rescala.extra.distributables.LociDist
import viscel.Viscel
import viscel.shared.Bindings

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.collection.mutable
import scala.concurrent.duration.*
import scala.concurrent.{Await, Promise}
import scala.jdk.CollectionConverters.*

class JettyServer(
    terminate: () => Unit,
    pages: ServerPages,
    staticPath: File,
    urlPrefix: String,
) {

  lazy val jettyServer: Server = {
    val threadPool = new QueuedThreadPool(4)
    threadPool.setName("http server")
    new Server(threadPool)
  }

  def stop(): Unit = jettyServer.stop()
  def start(interface: String, port: Int): Unit = {

    // connectors accept requests – in this case on a TCP socket
    val connector = new ServerConnector(jettyServer)
    jettyServer.addConnector(connector)
    connector.setHost(interface)
    connector.setPort(port)

    val zip = new GzipHandler()
    zip.addExcludedPaths("/blob/*")
    zip.setHandler(new HandlerList(mainHandler, staticResourceHandler, wsSetup()))

    jettyServer.setHandler(zip)

    jettyServer.start()
  }

  val staticResourceHandler = {
    // Create and configure a ResourceHandler.
    val handler = new ResourceHandler()
    // Configure the directory where static resources are located.
    handler.setBaseResource(Resource.newResource(staticPath.pathAsString))
    // Configure directory listing.
    handler.setDirectoriesListed(false)
    // Configure whether to accept range requests.
    handler.setAcceptRanges(true)
    handler
  }

  val landingString: String = pages.fullrender(pages.landingTag)
  val toolsString: String   = pages.fullrender(pages.toolsPage)

  def wsSetup() = {

    val wspath     = "/ws"
    val properties = Properties(heartbeatDelay = 3.seconds, heartbeatTimeout = 10.seconds)

    val registry = new Registry
    registry.bind(Bindings.version)(Viscel.version)

    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
    context.setContextPath(urlPrefix)
    jettyServer.setHandler(context)

    registry.listen(WS(context, wspath, properties))
    context
  }

  sealed trait Handling derives CanEqual
  case class Res(content: String, ct: String = "text/html; charset=UTF-8", status: Int = 200) extends Handling
  case object Unhandled                                                                       extends Handling

  object mainHandler extends AbstractHandler {
    override def handle(
        target: String,
        baseRequest: Request,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Unit = {

      def isPost  = HttpMethod.POST.is(request.getMethod)

      val res = {
        if (isPost) {
          request.getRequestURI match {
            case "/stop" =>
              terminate()
              Res("")
            case other => Unhandled
          }

        } else request.getRequestURI match {
          case "/"        => Res(landingString)
          case "/version" => Res(Viscel.version, "text/plain; charset=UTF-8")
          case "/tools"   => Res(toolsString)
          case other      => Unhandled
        }
      }

      res match {
        case Res(str, ct, status) =>
          response.setStatus(status)
          response.setContentType(ct)
          response.getWriter.println(str)
          baseRequest.setHandled(true)
        case Unhandled =>
      }
    }

  }

}
