package visceljs

import scala.scalajs.js.URIUtils.encodeURIComponent
import scala.scalajs.js.URIUtils.decodeURIComponent

sealed abstract class AppState(val urlhash: String) derives CanEqual {
  def transformPos(f: Int => Int) =
    this match {
      case other              => other
    }
  def position: Int =
    this match {
      case _                  => 0
    }
}
object AppState {
  def parse(path: String): AppState = {
    val paths = path.substring(1).split("/").toList
    paths match {
      case Nil | "" :: Nil => IndexState
      case _ => IndexState
    }
  }

  case object IndexState                  extends AppState("")
}
