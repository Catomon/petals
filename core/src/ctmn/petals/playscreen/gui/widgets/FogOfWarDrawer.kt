package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playscreen.events.NextTurnEvent
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.Const.BASE_RANGE_OF_VIEW
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playstage.*
import ctmn.petals.tile.Tiles
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.isBase
import ctmn.petals.unit.*
import ctmn.petals.unit.component.InvisibilityComponent
import kotlin.Array as KArray

class FogOfWarDrawer(val playScreen: PlayScreen) : Actor() {

    private val playStage = playScreen.playStage

    private val fogOfWarSprite: Sprite = Sprite(playScreen.assets.textureAtlas.findRegion("gui/tile_black"))

    var fogMap: KArray<IntArray>? = null

    var drawFog = false
        set(value) {
            field = value

            updateFog = true
        }

    var drawDiscoverableFog = false

    var fogDiscoverMap = arrayOf<IntArray>()

    var hideAll = false
        set(value) {
            field = value

            updateFog = true
        }

    private var updateFog = true

    init {
        fogOfWarSprite.setSize(Const.TILE_SIZE.toFloat(), Const.TILE_SIZE.toFloat())
        fogOfWarSprite.setAlpha(0.5f)

        playStage.addListener {
            when (it) {
                is CommandExecutedEvent -> {
                    updateFog = true
                }
                is NextTurnEvent -> {
                    updateFog = true
                }
                is ActionCompletedEvent -> {
                    updateFog = true
                }
            }

            false
        }
    }

    fun update() {
        fogMap = if(!drawFog) null else getFogOfWar()

        for (x in fogDiscoverMap.indices) {
            for (y in fogDiscoverMap[x].indices) {
                if (isVisible(x, y))
                    fogDiscoverMap[x][y] = 1
            }
        }
    }

    fun updateGridMap() {
        fogDiscoverMap = playStage.getMapSizedGridOfZero()
    }

    fun isVisible(tileX: Int, tileY: Int) : Boolean {
        if(hideAll) return false

        if (!drawFog) return true

        if (fogMap == null) return true

        return fogMap!![tileX][tileY] > 0
    }

    private fun getFogOfWar() : KArray<IntArray> {
        val maxOfView = playStage.getMapSizedGridOfZero()
        for (unit in playStage.getUnitsForTeam(playScreen.localPlayer.teamId)) {
            if (hideAll)
                break

            val unitViewRangeMatrix = playStage.getRangeOfView(unit)
            for ((i, _) in maxOfView.withIndex()) {
                for ((j, _) in maxOfView[i].withIndex()) {
                    if (unitViewRangeMatrix[i][j] > 0)
                        maxOfView[i][j] += unitViewRangeMatrix[i][j]
                }
            }
        }

        //vision of castles
        for (tile in playStage.getTiles()) {
            if (hideAll)
                break

            //temp lambda fix
            if (tile.isBase && playScreen.turnManager.players.find { it.id == tile.cPlayerId?.playerId }?.teamId == playScreen.localPlayer.teamId) {
                val castle = tile
                val castleViewRangeMatrix = playStage.getMovementGrid(
                    BASE_RANGE_OF_VIEW, castle.tiledX, castle.tiledY, TerrainPropsPack.view)
                for ((i, _) in maxOfView.withIndex()) {
                    for ((j, _) in maxOfView[i].withIndex()) {
                        if (castleViewRangeMatrix[i][j] > 0)
                            maxOfView[i][j] += castleViewRangeMatrix[i][j]
                    }
                }
            }
        }

        for (tile in playStage.getTiles())
            if (tile.selfName == Tiles.EDGE)
                maxOfView[tile.tiledX][tile.tiledY] = 1

        for ((i, _) in maxOfView.withIndex()) {
            for ((j, _) in maxOfView[i].withIndex()) {
                if (playStage.getTile(i, j) == null)
                    maxOfView[i][j] = 1

            }
        }

        return maxOfView
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        if (updateFog) {
            update()

            updateFog = false
        }

        //reset alpha
        for (unit in playStage.getUnits()) {
            unit.isVisible = true
            unit.color.a = 1f

            //update invisibility
            if (unit.has(InvisibilityComponent::class.java)) {
                if (unit.isPlayerTeamUnit(playScreen.localPlayer))
                    unit.color.a = 0.6f
                else
                    unit.isVisible = false
            }
        }

        if (!drawFog)
            return

        //vision of units
        val maxOfView = fogMap ?: return

        //draw
        fogOfWarSprite.setAlpha(0.5f)
        for ((i, _) in maxOfView.withIndex()) {
            for ((j, _) in maxOfView[i].withIndex()) {
                if (maxOfView[i][j] <= 0) {
                    fogOfWarSprite.setPosition(i * Const.TILE_SIZE.toFloat(), j * Const.TILE_SIZE.toFloat())
                    fogOfWarSprite.draw(batch)

                    //todo getUnit(x, y) (map array)
                    for (unit in playStage.getUnits()) { //TODO
                        if (hideAll || unit.teamId != playScreen.localPlayer.teamId)
                            if (unit.tiledX == i && unit.tiledY == j)
                                unit.isVisible = false
                    }
                }
            }
        }

        if (!drawDiscoverableFog) return

        fogOfWarSprite.setAlpha(1f)
        for (x in fogDiscoverMap.indices) {
            for (y in fogDiscoverMap[x].indices) {
                if (fogDiscoverMap[x][y] == 1) continue

                fogOfWarSprite.setPosition(x * Const.TILE_SIZE.toFloat(), y * Const.TILE_SIZE.toFloat())
                fogOfWarSprite.draw(batch)
            }
        }
    }
}
