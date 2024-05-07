package ctmn.petals.playscreen.tasks

import com.badlogic.gdx.utils.Array
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playstage.getUnitsOfEnemyOf
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.isAlive

class EliminateAllEnemyUnitsTask(
    private val enemyUnits: Array<UnitActor>? = null,
) : Task() {

    override var description: String? = "Eliminate all enemy units"

    override fun update(delta: Float) {
        isCompleted =
            enemyUnits?.none { it.isAlive() } ?: playScreen.playStage.getUnitsOfEnemyOf(playScreen.localPlayer).isEmpty
    }
}