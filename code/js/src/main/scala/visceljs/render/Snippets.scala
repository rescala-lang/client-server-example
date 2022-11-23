package visceljs.render

import org.scalajs.dom
import org.scalajs.dom.html.Element
import rescala.default._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{alt, stringFrag, _}
import scalatags.JsDom.tags2.{nav, section}
import visceljs.Definitions._
import visceljs.{Definitions, Icons, MetaInfo}

sealed trait FitType derives CanEqual {
  def next: FitType =
    this match {
      case FitType.W   => FitType.WH
      case FitType.WH  => FitType.O
      case FitType.O   => FitType.SW
      case FitType.SW  => FitType.SWH
      case FitType.SWH => FitType.W
    }
}
object FitType {
  case object W   extends FitType
  case object WH  extends FitType
  case object O   extends FitType
  case object SW  extends FitType
  case object SWH extends FitType
}

object Snippets {


  def fullscreenToggle(stuff: Modifier*): HtmlTag = lcButton(Definitions.toggleFullscreen())(stuff: _*)

  def navigation(links: Modifier*): HtmlTag = nav(links: _*).asInstanceOf[HtmlTag]

  def meta(meta: MetaInfo): Signal[TypedTag[Element]] = {
    val connectionStatus = Signal {
      meta.connection.value match {
        case 0     => stringFrag(s"disconnected (attempt № ${meta.reconnecting.value})")
        case other => stringFrag(s"$other active")
      }
    }
    Signal {
      section(List[Frag](
        s"app version: ${meta.version}",
        br(),
        s"server version: ",
        meta.remoteVersion.value,
        br(),
        s"service worker: ",
        meta.serviceState.value,
        br(),
        s"connection status: ",
        connectionStatus.value,
        br()
      )).asInstanceOf[HtmlTag]
    }
  }

}
