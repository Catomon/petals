package ctmn.petals.playscreen

import ctmn.petals.Const
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor
import ctmn.petals.actors.actions.OneAction
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import ctmn.petals.map.label.LabelActor
import ctmn.petals.playstage.PlayStage
import ctmn.petals.unit.cUnit

fun Actor.doActionAndThen(action: Action, thenAction: Action.() -> Unit) {
    addAction(Actions.sequence(action, OneAction(thenAction)))
}

/** to identify unit or tile type */
val Actor.selfName: String get() {
    return when(this) {
        is UnitActor -> cUnit.name
        is TileActor -> tileName
        is LabelActor -> labelName
        else -> name.split("@")[0]
    }
}

/** to identify concrete actor on stage */
val Actor.stageName: String get() = name

val Actor.playStage get() = stage as PlayStage

val Actor.playStageOrNull get() = if (stage is PlayStage) stage as PlayStage else null

fun TileActor.setTilePosition(x: Int, y: Int) {
    this.x = (x * Const.TILE_SIZE).toFloat()
    this.y = (y * Const.TILE_SIZE).toFloat()
    tiledX = x
    tiledY = y
}
