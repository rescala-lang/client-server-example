package visceljs.connection

import java.util.NoSuchElementException

import com.github.plokhotnyuk.jsoniter_scala.core._
import loci.communicator.ws.webnative.WS
import loci.registry.Registry
import loci.transmitter.RemoteRef
import org.scalajs.dom
import rescala.default._
import rescala.operator.RExceptions.EmptySignalControlThrowable
import viscel.shared._
import visceljs.ViscelJS.fetchbuffer
import visceljs.storage.{LocalForageInstance, Storing, localforage}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.{Int8Array, TypedArrayBuffer}

class ContentConnectionManager(registry: Registry) {

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  val wsUri: String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${dom.document.location.pathname}ws"
  }

  val joined = Events.fromCallback[RemoteRef] { cb =>
    registry.remoteJoined.foreach(cb)
  }.event
  val left = Events.fromCallback[RemoteRef] { cb =>
    registry.remoteLeft.foreach(cb)
  }.event

  val connectionStatusChanged = joined || left

  val connectedRemotes = connectionStatusChanged.fold[List[RemoteRef]](Nil) { (_, _) =>
    registry.remotes.filter(_.connected)
  }

  val connectionStatus: Signal[Int] = connectedRemotes.map(_.size)

  val mainRemote = connectedRemotes.map(_.headOption.getOrElse(throw EmptySignalControlThrowable))

  val remoteVersion: Signal[String] =
    (joined.map(rr => Signals.fromFuture(registry.lookup(Bindings.version, rr)))
      .latest(Signal { "unknown" })).flatten
      .recover(e => s"error »$e«")

  val connectionAttempt: Evt[Unit] = Evt[Unit]()

  val reconnecting: Signal[Int] = Fold(0)(
    connectionAttempt act { _ => current + 1 },
    joined act { _ => 0 }
  )

  def connect(): Future[RemoteRef] = {
    println(s"trying to connect to $wsUri")
    connectionAttempt.fire()
    registry.connect(WS(wsUri))
  }

  def connectLoop(): Unit = {
    connect().failed.foreach { err =>
      println(s"connection failed »$err«")
      dom.window.setTimeout(() => connectLoop(), 10000)
    }
  }

  def autoreconnect(): Unit = {
    left.filter(_ => connectionStatus.value == 0).observe(_ => connectLoop())
    connectLoop()
  }

  val lfi: LocalForageInstance = localforage.createInstance(js.Dynamic.literal("name" -> "contents"))

}
