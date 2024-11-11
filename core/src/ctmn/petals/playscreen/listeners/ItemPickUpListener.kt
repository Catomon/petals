package ctmn.petals.playscreen.listeners

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.kotcrab.vis.ui.widget.VisImage
import ctmn.petals.effects.FloatingUpIconLabel
import ctmn.petals.effects.FloatingUpLabel
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.CreditsChangeEvent
import ctmn.petals.playscreen.events.UnitDiedEvent
import ctmn.petals.playscreen.events.UnitMovedEvent
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.removeTileSafely
import ctmn.petals.unit.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class ItemPickUpListener(val playScreen: PlayScreen) : EventListener {
    override fun handle(event: Event): Boolean {
        if (event is UnitMovedEvent) {
            val unit = event.unit
            unit.playStageOrNull?.let { playStage ->
                playStage.getTile(unit.tiledX, unit.tiledY)?.let { tile ->
                    if (tile.selfName.startsWith(ctmn.petals.tile.Tiles.CRYSTAL_SHARD)) {
                        val player = playScreen.turnManager.getPlayerById(unit.playerId) ?: return false
                        player.credits += 150
                        playStage.addActor(FloatingUpIconLabel("+150", "credits", 15f).also { it.label.color = Color.SKY; it.setPosition(unit.centerX, unit.centerY)})
                        playScreen.fireEvent(CreditsChangeEvent(player, 150))
                        playStage.removeTileSafely(tile)
                    }
                }
            }
        }

        return false
    }
}