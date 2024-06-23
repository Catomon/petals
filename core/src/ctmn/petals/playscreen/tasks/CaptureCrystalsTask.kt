package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.TaskUpdatedEvent
import ctmn.petals.playscreen.events.TileCapturedEvent
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getTiles
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.isCrystal
import ctmn.petals.utils.addOneTimeListener

class CaptureCrystalsTask(
    var amountNeeded: Int = -1,
) : Task() {

    override var description: String? = "Capture crystal tiles"
    lateinit var crystalsOnMap: List<TileActor>

    override fun onBegin(playScreen: PlayScreen) {
        super.onBegin(playScreen)
        crystalsOnMap = playScreen.playStage.getTiles().filter { tile -> tile.isCrystal }
        amountNeeded = if (amountNeeded > 0) amountNeeded else crystalsOnMap.size

        description = "Capture crystal tiles 0/$amountNeeded"

        playScreen.playStage.addOneTimeListener<TileCapturedEvent> {
            val playerCrystalsAmount =
                playScreen.playStage.getCapturablesOf(playScreen.localPlayer).filter { tile -> tile.isCrystal }.size
            isCompleted = playerCrystalsAmount == amountNeeded

            description = "Capture crystal tiles $playerCrystalsAmount/$amountNeeded"

            playScreen.guiStage.tasksTable.fire(TaskUpdatedEvent(this@CaptureCrystalsTask))

            return@addOneTimeListener isCompleted
        }
    }

    override fun onCompleted() {
        super.onCompleted()
    }

    override fun update(delta: Float) {

    }
}