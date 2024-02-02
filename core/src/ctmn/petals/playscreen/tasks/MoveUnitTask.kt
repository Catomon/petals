package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

class MoveUnitTask(
    val unit: UnitActor,
    val x: Int, val y: Int,
    forcePlayerToComplete: Boolean = false
) : Task() {

    override var description: String? = "Move unit."

    init {
        this.isForcePlayerToComplete = forcePlayerToComplete
    }

    override fun update(delta: Float) {
        if (isForcePlayerToComplete)
            playScreen.commandManager.getNextInQueue()?.also {
                if (it is MoveUnitCommand && playScreen.playStage.root.findActor<UnitActor>(it.unitId) == unit && it.tileX == x && it.tileY == y)
                    playScreen.commandManager.stop = false
                else
                    playScreen.commandManager.clearQueue()
            }

        if (unit.tiledX == x && unit.tiledY == y && playScreen.actionManager.isQueueEmpty) {
            isCompleted = true
        }
    }

    override fun onBegin(playScreen: PlayScreen) {
        super.onBegin(playScreen)

        if (isForcePlayerToComplete) {
            playScreen.commandManager.stop = true
        }
    }
}