package viscel

import java.lang.management.ManagementFactory
import java.nio.file.{Path, Paths}
import java.io.File as jFile

import better.files.File

import scala.collection.immutable.ArraySeq

import com.softwaremill.quicklens.{modifyLens as path, *}

object Viscel {

  extension [A, C](inline oparse: scopt.OParser[A, C]) {
    inline def lens(inline path: PathLazyModify[C, A]) = oparse.mlens(path, identity)
    inline def mlens[B](inline path: PathLazyModify[C, B], map: A => B) =
      oparse.action((a, c) => (path.setTo(map(a))(c)))
    inline def vlens[B](inline path: PathLazyModify[C, B], value: B) =
      oparse.action((a, c) => (path.setTo(value)(c)))
  }

  case class Args(
      optBasedir: Path = Paths.get("./data"),
      port: Int = 2358,
      interface: String = "0",
      server: Boolean = true,
      shutdown: Boolean = false,
      optStatic: Path = Paths.get("static"),
      urlPrefix: String = "",
  )

  val soptags = {
    val builder = scopt.OParser.builder[Args]
    import builder.*
    scopt.OParser.sequence(
      programName("viscel"),
      head("Start viscel!"),
      help('h', "help").hidden(),
      opt[Int]("port").text("Weberver listening port.").lens(path(_.port)),
      opt[String]("interface").valueName("interface").text("Interface to bind the server to.").lens(path(_.interface)),
      opt[Unit]("noserver").text("Do not start the server.").vlens(path(_.server), false),
      opt[Unit]("shutdown").text("Shutdown directly.").vlens(path(_.shutdown), true),
      opt[jFile]("static").valueName("directory").text("Directory of static resources.")
        .mlens(path(_.optStatic), _.toPath),
      opt[String]("urlprefix").text("Prefix for server URLs.").lens(path(_.urlPrefix)),
    )
  }

  def makeService(args: Args): Services = {
    import args.*
    val staticCandidates = List(File(optBasedir.resolve(optStatic)), File(optStatic))

    val staticDir =
      staticCandidates.find(_.isDirectory)
        .getOrElse {
          println(s"Missing optStatic resource directory, " +
            s"tried ${staticCandidates.map(c => s"??$c??").mkString(", ")}.")
          sys.exit(0)
        }

    val services = new Services(staticDir.path, urlPrefix, interface, port)


    if (server) {
      println(s"starting server")
      services.startServer()
    }

    if (shutdown) {
      services.terminateEverything(server)
    }
    println(s"initialization done in ${ManagementFactory.getRuntimeMXBean.getUptime}ms")
    services
  }

  val version: String = viscel.shared.BuildInfo.version

  def main(args: Array[String]): Unit = {
    run(ArraySeq.unsafeWrapArray(args): _*)
    ()
  }

  def run(args: String*): Option[Services] = {
    println(s"Viscel version $version")
    scopt.OParser.parse(soptags, args, Args()).map(makeService)
  }
}
