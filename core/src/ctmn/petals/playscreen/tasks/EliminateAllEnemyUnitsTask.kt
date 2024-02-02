package ctmn.petals.playscreen.tasks

import ctmn.petals.playstage.getUnitsOfEnemyOf

class EliminateAllEnemyUnitsTask : Task() {

    override var description: String? = "Eliminate all enemy units"

    override fun update(delta: Float) {
        isCompleted = playScreen.playStage.getUnitsOfEnemyOf(playScreen.localPlayer).isEmpty
    }
}