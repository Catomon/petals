package ctmn.petals.playscreen.listeners

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.sim.BattleField
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.tile.cLifeTime
import ctmn.petals.tile.cReplaceWith
import ctmn.petals.tile.components.LifeTimeComponent

class TileLifeTimeListener(val field: BattleField) : EventListener {

    override fun handle(event: Event): Boolean {
        if (event is TurnsCycleListener.TurnCycleEvent) {

            for (tile in field.allTiles.toList()) {
                var lifetime = tile.cLifeTime?.lifeTime ?: continue
                lifetime -= event.turnCycleTime

                tile.add(LifeTimeComponent(lifetime))

                if (lifetime <= 0) {
                    field.removeGameActor(tile)

                    tile.cReplaceWith?.tileName?.let {
                        val replTileData = TileData.get(it as String) ?: return@let
                        val replTile = TileActor(replTileData.name, replTileData.terrain)
                        replTile.tileComponent.layer = tile.layer
                        replTile.setPosition(tile.tiledX, tile.tiledY)
                        field.addGameActor(replTile)
                    }
                }
            }
        }

        return false
    }
}
