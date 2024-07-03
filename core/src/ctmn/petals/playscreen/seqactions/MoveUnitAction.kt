package ctmn.petals.playscreen.seqactions

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import ctmn.petals.Const
import ctmn.petals.assets
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.UnitMovedEvent
import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.isAir
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

class MoveUnitAction(val unit: UnitActor, val startX: Int, val startY: Int, val endX: Int, val endY: Int) :
    SeqAction() {

    private val queAction = SequenceAction()

    constructor(unit: UnitActor, endX: Int, endY: Int) : this(unit, unit.tiledX, unit.tiledY, endX, endY)

    override fun update(deltaTime: Float) {
        if (!unit.actions.contains(queAction)) {
            unit.setPosition(endX, endY)

            isDone = true

            playScreen.fireEvent(UnitMovedEvent(unit, startX, startY))

            if (playScreen.fogOfWarManager.isVisible(endX, endY))
                if (unit.isAir) {
                    assets.getSound("flee.ogg").play()
                } else {
                    when (playScreen.playStage.getTile(endX, endY)?.terrain) {
                        TerrainNames.grass -> {
                            assets.getSound("step_grass.ogg").play()
                        }

                        TerrainNames.mountains, TerrainNames.skyscraper -> {
                            assets.getSound("step_rock.ogg").play()
                        }

                        TerrainNames.water, TerrainNames.swamp, TerrainNames.deepwater -> {
                            assets.getSound("step_water.ogg").play()
                        }

                        TerrainNames.forest -> {
                            assets.getSound(arrayOf("step_forest.ogg", "step_forest_1.ogg").random()).play()
                        }

                        else -> {
                            assets.getSound("step_grass.ogg").play()
                        }
                    }
                }
        }
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        var distX = endX * Const.TILE_SIZE - unit.x
        var distY = endY * Const.TILE_SIZE - unit.y
        if (distX < 0)
            distX *= -1f
        if (distY < 0)
            distY *= -1f
        val moveActionX = Actions.moveTo(
            endX * Const.TILE_SIZE, unit.y,
            distX / Const.UNIT_MOVE_SPEED
        )
        val moveActionY = Actions.moveTo(
            endX * Const.TILE_SIZE,
            endY * Const.TILE_SIZE, distY / Const.UNIT_MOVE_SPEED
        )

        queAction.addAction(moveActionX)
        queAction.addAction(moveActionY)

        unit.addAction(queAction)

        return true
    }
}