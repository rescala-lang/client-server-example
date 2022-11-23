package visceljs

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.scalajs.dom
import org.scalajs.dom.{HashChangeEvent, URL, html}
import rescala.default.{Event, Events, Fold, Signal, current}
import scalatags.JsDom.TypedTag
import visceljs.AppState.IndexState
import visceljs.Navigation.{Mode, Next, Position, Prev, navigationEvents}
import visceljs.render.OverviewPage
import visceljs.storage.Storing

import scala.collection.immutable.Map

class ReaderApp() {

  def getHash(): String = dom.window.location.hash

  def makeBody(index: OverviewPage): Signal[Option[TypedTag[html.Body]]] = {

    val hashChange: Event[HashChangeEvent] =
      Events.fromCallback[HashChangeEvent](dom.window.onhashchange = _).event
    hashChange.observe(hc => println(s"hash change event: ${hc.oldURL} -> ${hc.newURL}"))

    val targetStates = hashChange.map(hc => AppState.parse(new URL(hc.newURL).hash))

    val initialAppState                         = AppState.parse(getHash())
    val currentTargetAppState: Signal[AppState] = targetStates.fold(initialAppState) { case (_, next) => next }

    val normalizedAppState: Signal[AppState] =
      Signal { currentTargetAppState.value }

    normalizedAppState.observe(
      fireImmediately = false,
      onValue = { as =>
        val nextHash    = as.urlhash
        val currentHash = getHash().drop(1)
        if (nextHash != currentHash) {
          println(s"pushing ${nextHash} was $currentHash")
          dom.window.history.pushState(null, null, s"#${as.urlhash}")
        }
      }
    )

    Signal {
      currentTargetAppState.value match {
        case IndexState    => Some("Viscel")
      }
    }.observe {
      case Some(newTitle) => dom.window.document.title = newTitle
    }

    val indexBody = index.gen()


    currentTargetAppState.map {
      case IndexState      => Signal(Some(indexBody))
    }.flatten
  }

}
