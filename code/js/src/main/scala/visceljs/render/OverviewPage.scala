package visceljs.render

import org.scalajs.dom
import org.scalajs.dom.{Event, html}
import rescala.default._
import rescala.extra.Tags._
import scalatags.JsDom
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import scalatags.JsDom.implicits.stringFrag
import visceljs.Definitions.link_tools
import visceljs.{Definitions, MetaInfo, SearchUtil}

import scala.collection.immutable.Map

class OverviewPage(
    meta: MetaInfo,
) {

  def gen(): TypedTag[html.Body] = {
    body(
      id := "index",
      Snippets.meta(meta).asModifier,
      Snippets.navigation(Snippets.fullscreenToggle("fullscreen"), link_tools("tools")),
    )
  }

}
