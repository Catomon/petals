package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.events.UnitSelectedEvent
import ctmn.petals.unit.*
import ctmn.petals.utils.setPositionByCenter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.utils.Array
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getLeadUnit
import ctmn.petals.playstage.getMovementGrid
import ctmn.petals.playstage.getTiles
import ctmn.petals.unit.UnitActor

class LeaderFieldDrawer(private val guiStage: PlayGUIStage) : Actor() {

    val sprite = Sprite(guiStage.assets.atlases.findRegion("gui/leader_field"))
    private val borders = Array<Pair<Int, Int>>()

    init {
        sprite.setSize(Const.TILE_SIZE.toFloat(), Const.TILE_SIZE.toFloat())

        //create leader field border if an unit selected
        guiStage.addListener {
            if (it is UnitSelectedEvent) {
                makeForUnit(it.unit)
            }

            false
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        if (actions.isEmpty) {
            addAction(Actions.sequence(Actions.alpha(0.1f, 1f), Actions.alpha(0.65f, .85f)))
        }

        when (isVisible) {
            true -> {
                if (!guiStage.playScreen.actionManager.isQueueEmpty)
                    isVisible = false
            }
            false -> {
                if (guiStage.selectedUnit != null && guiStage.playScreen.actionManager.isQueueEmpty) {
                    isVisible = true
                    makeForUnit(guiStage.selectedUnit)
                }
            }
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        if (guiStage.isUnitSelected) {
            for (border in borders) {
                sprite.setPositionByCenter(border.first * Const.TILE_SIZE + Const.TILE_SIZE.toFloat() / 2,
                        border.second * Const.TILE_SIZE + Const.TILE_SIZE.toFloat() / 2)
                sprite.draw(batch, color.a)
            }
        }
    }

    private fun makeForUnit(unit: UnitActor?) {
        unit ?: return

        //get selectedUnit leaderId; return and hide if selectedUnit have no leaderC nor followerC
        val leaderId =
            when {
                unit.isLeader -> unit.leaderID
                unit.isFollower -> unit.followerID
                else -> { isVisible = false; return }
            }

        //return and hide if no selected unit leader found
        val leader = guiStage.playStage.getLeadUnit(leaderId)
        if (leader == null) {
            isVisible = false
            return
        } else {
            isVisible = true
            if (!unit.isPlayerUnit(guiStage.localPlayer)) {
                sprite.color = Color.RED
            } else sprite.color = Color.WHITE

            //color.a = 0.5f
            //actions.clear()
        }

        makeForRange(leader.cLeader!!.leaderRange, leader.tiledX, leader.tiledY, guiStage.playStage)
    }

    private fun makeForRange(range: Int, x: Int, y: Int, engine: PlayStage) {
        makeForMatrix(engine.getMovementGrid(range, x, y, TerrainPropsPack.ability), engine)
    }

    private fun makeForMatrix(matrix: kotlin.Array<IntArray>, engine: PlayStage) {
        borders.clear()

        for (tile in engine.getTiles()) {
            if (matrix[tile.tiledX][tile.tiledY] > 0) {
                borders.add(tile.tiledX to tile.tiledY)
            }
        }
        setSize(8f, 8f)
        addAction(Actions.sizeTo(Const.TILE_SIZE.toFloat(), Const.TILE_SIZE.toFloat(), 0.15f))
    }
}
