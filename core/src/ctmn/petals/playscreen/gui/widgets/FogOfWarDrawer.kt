package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.Const
import ctmn.petals.Const.BASE_RANGE_OF_VIEW
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.playstage.*
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.isBase
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.component.InvisibilityComponent
import ctmn.petals.unit.isPlayerTeamUnit
import ctmn.petals.unit.teamId
import kotlin.Array as KArray

class FogOfWarDrawer(val playScreen: PlayScreen) : Actor() {

    private val playStage = playScreen.playStage

    private val fogOfWarSprite: Sprite = Sprite(playScreen.assets.atlases.findRegion("misc/fow"))
    private val spriteAlpha = 0.40f

    var fogMap: KArray<IntArray>? = null

    var drawFog = false
        get() = field || hideAll
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
        fogOfWarSprite.setSize(Const.TILE_SIZE, Const.TILE_SIZE)
        fogOfWarSprite.setAlpha(spriteAlpha)

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
        fogMap = if (!drawFog) null else getFogOfWar()

        for (x in fogDiscoverMap.indices) {
            for (y in fogDiscoverMap[x].indices) {
                if (fogMap != null) {
                    val fogMap = fogMap!!
                    val fogMapX = x - 1
                    val fogMapY = y - 1
                    if (fogMapX in fogMap.indices && fogMapY in fogMap[0].indices) {
                        if (isVisible(fogMapX, fogMapY))
                            fogDiscoverMap[x][y] = 1
                    } else {
                        if (isVisible(fogMapX + 1, fogMapY) || isVisible(fogMapX - 1, fogMapY) || isVisible(fogMapX, fogMapY + 1) || isVisible(fogMapX, fogMapY - 1))
                            fogDiscoverMap[x][y] = 1
                    }
                } else {
                    fogDiscoverMap[x][y] = 1
                }
            }
        }
    }

    fun updateGridMap() {
        val mapSizedGrid = playStage.getMapSizedGridOfZero()
        fogDiscoverMap = KArray(mapSizedGrid.size + 2) {
            IntArray(mapSizedGrid[0].size + 2) {
                0
            }
        }
    }

    fun isVisible(tileX: Int, tileY: Int): Boolean {
        if (hideAll) return false

        if (!drawFog) return true

        if (fogMap == null) return true

        if (tileX !in 0 until fogMap!!.size) return false

        if (tileY !in 0 until fogMap!![tileX].size) return false

        return fogMap!![tileX][tileY] > 0
    }

    private fun getFogOfWar(): KArray<IntArray> {
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
                    BASE_RANGE_OF_VIEW, castle.tiledX, castle.tiledY, TerrainPropsPack.view
                )
                for ((i, _) in maxOfView.withIndex()) {
                    for ((j, _) in maxOfView[i].withIndex()) {
                        if (castleViewRangeMatrix[i][j] > 0)
                            maxOfView[i][j] += castleViewRangeMatrix[i][j]
                    }
                }
            }
        }

//        for (tile in playStage.getTiles())
//            if (tile.selfName == Tiles.EDGE)
//                maxOfView[tile.tiledX][tile.tiledY] = 1

//        for ((i, _) in maxOfView.withIndex()) {
//            for ((j, _) in maxOfView[i].withIndex()) {
//                if (playStage.getTile(i, j) == null)
//                    maxOfView[i][j] = 1
//
//            }
//        }

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
        fogOfWarSprite.setAlpha(spriteAlpha)
        for ((i, _) in maxOfView.withIndex()) {
            for ((j, _) in maxOfView[i].withIndex()) {
                if (maxOfView[i][j] <= 0) {
                    fogOfWarSprite.setPosition(i * Const.TILE_SIZE, j * Const.TILE_SIZE)
                    fogOfWarSprite.draw(batch)

                    playStage.getUnit(i, j)?.let { unit ->
                        if (hideAll || unit.teamId != playScreen.localPlayer.teamId)
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

                fogOfWarSprite.setPosition((x - 1) * Const.TILE_SIZE, (y - 1) * Const.TILE_SIZE)
                fogOfWarSprite.draw(batch)
            }
        }
    }
}
