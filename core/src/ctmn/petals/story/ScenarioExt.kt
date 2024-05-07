package ctmn.petals.story

import ctmn.petals.playscreen.GameEndCondition
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.addTrigger
import ctmn.petals.playstage.getUnit
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.unit.actors.Alice

fun PlayScreen.gameOverSuccess() {
    gameEndCondition.result = GameEndCondition.Result.WIN
    gameOver()
}

fun PlayScreen.gameOverFailure() {
    gameEndCondition.result = GameEndCondition.Result.LOSE
    gameOver()
}

fun PlayScreen.addAliceDiedGameOverTrigger(alice: Alice? = null) {
    addTrigger(UnitsDiedTrigger(alice ?: alice())).onTrigger {
        gameOverFailure()
    }
}

/** Find Alice on the stage */
fun PlayScreen.alice() : Alice = playStage.getUnit<Alice>() ?: throw IllegalStateException("Alissa is not on the Stage")

fun PlayScreen.aliceOrNull() : Alice? = playStage.getUnit<Alice>()


fun Scenario.playScreen(playScreenApply: PlayScreen.() -> Unit ) {
    playScreenApply(playScreen)
}