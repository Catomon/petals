package ctmn.petals.playscreen.listeners

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.playscreen.events.CreditsChangeEvent
import ctmn.petals.playscreen.events.UnitMovedEvent
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.removeTileSafely
import ctmn.petals.sim.Battle
import ctmn.petals.story.StoryPlayScreen
import ctmn.petals.unit.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class ItemPickUpListener(val battle: Battle) : EventListener {
    override fun handle(event: Event): Boolean {
        if (event is UnitMovedEvent) {
            val unit = event.unit
            val tile = battle.field.getTile(unit.tiledX, unit.tiledY)
            if (tile != null && tile.selfName.startsWith(ctmn.petals.tile.Tiles.CRYSTAL_SHARD)) {
                val player = battle.turnManager.getPlayerById(unit.playerId) ?: return false
                if (battle is StoryPlayScreen && player.id != battle.localPlayer.id) return false

                player.credits += 150
                battle.showFloatingIconLabel("+150", "credits", unit.centerX, unit.centerY)
                battle.fireEvent(CreditsChangeEvent(player, 150))
                battle.field.removeTileSafely(tile)
            }
        }

        return false
    }
}
