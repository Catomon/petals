package ctmn.petals.story

import ctmn.petals.playscreen.GameEndCondition
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.addTrigger
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.playstage.getUnit
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.unit.actors.Alice

fun PlayScreen.gameOverSuccess() {
    gameEndCondition.win()

    if (!actionManager.hasActions)
        fireEvent(ActionCompletedEvent(WaitAction(0.5f)))
    //gameOver()
}

fun PlayScreen.gameOverFailure() {
    gameEndCondition.lose()

    if (!actionManager.hasActions)
        fireEvent(ActionCompletedEvent(WaitAction(0.5f)))
    //gameOver()
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