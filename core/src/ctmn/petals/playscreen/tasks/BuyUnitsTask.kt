package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.TaskUpdatedEvent
import ctmn.petals.playscreen.events.UnitBoughtEvent
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.utils.addOneTimeListener

class BuyUnitsTask(
    val unitName: String,
    var unitIdBuy: String,
    var unitsAmountNeeded: Int,
) : Task() {

    override var description: String? = "Buy $unitName units 0/$unitsAmountNeeded"
    var unitsBought = 0

    override fun onBegin(playScreen: PlayScreen) {
        super.onBegin(playScreen)

        playScreen.playStage.addOneTimeListener<UnitBoughtEvent> {
            if (unit.selfName == unitIdBuy) {
                unitsBought++
                description = "Buy $unitName units $unitsBought/$unitsAmountNeeded"
                isCompleted = unitsBought == unitsAmountNeeded

                playScreen.guiStage.tasksTable.fire(TaskUpdatedEvent(this@BuyUnitsTask))

            }

            return@addOneTimeListener isCompleted
        }
    }

    override fun onCompleted() {
        super.onCompleted()
    }

    override fun update(delta: Float) {

    }
}