package ctmn.petals.playscreen.listeners

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getAllTiles
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.tile.cLifeTime
import ctmn.petals.tile.cReplaceWith
import ctmn.petals.tile.components.LifeTimeComponent

class TileLifeTimeListener(val playStage: PlayStage) : EventListener {

    override fun handle(event: Event): Boolean {
        if (event is TurnsCycleListener.TurnCycleEvent) {

            for (tile in playStage.getAllTiles()) {
                var lifetime = tile.cLifeTime?.lifeTime ?: continue
                lifetime -= event.turnCycleTime

                tile.add(LifeTimeComponent(lifetime))

                if (lifetime <= 0) {
                    tile.remove()

                    tile.cReplaceWith?.tileName?.let {
                        val replTileData = TileData.get(it as String) ?: return@let
                        val replTile = TileActor(replTileData.name, replTileData.terrain)
                        replTile.tileComponent.layer = tile.layer
                        replTile.setPosition(tile.tiledX, tile.tiledY)
                        playStage.addActor(replTile)
                    }
                }
            }
        }

        return false
    }
}