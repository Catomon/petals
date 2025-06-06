package ctmn.petals.playscreen

import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.player.Player
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.seqactions.*
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.playscreen.triggers.*
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.utils.Array
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.tile.components.CrystalsComponent
import ctmn.petals.tile.isBase
import ctmn.petals.tile.isCrystal
import kotlin.math.max

fun PlayScreen.calcCreditsPerCluster(player: Player) = creditsPerCluster + max(0, playStage.getCapturablesOf(player).count { it.isBase } - 1) * 25

fun Player.income(playScreen: PlayScreen): Int {
    var income = if (creditsPassiveReserve > 0) playScreen.creditsPassiveIncome else 0
    playScreen.playStage.getCapturablesOf(this).forEach { tile ->
        if (tile.isBase) {
            //income += playScreen.creditsPerBase
        }
        else if (tile.isCrystal) income += playScreen.calcCreditsPerCluster(this)
    }

    return income
}

fun Player.incomeReserve(playScreen: PlayScreen): Int {
    var incomeReserve = creditsPassiveReserve
    playScreen.playStage.getCapturablesOf(this).forEach { tile ->
        if (tile.isCrystal) also {
            incomeReserve += tile.get(CrystalsComponent::class.java)?.amount ?: 0
        }
        else if (tile.isCrystal) incomeReserve += playScreen.creditsPerCluster
    }

    return incomeReserve
}

fun PlayScreen.queueAction(action: () -> Unit): SeqAction {
    return queueAction(OneSeqAction(action))
}

fun <T : SeqAction> PlayScreen.queueAction(action: T): T {
    actionManager.queueAction(action)

    return action
}

fun PlayScreen.addAction(action: () -> Unit): SeqAction {
    return addAction(OneSeqAction(action))
}

fun PlayScreen.addAction(action: SeqAction): SeqAction {
    actionManager.addAction(action)

    return action
}

fun PlayScreen.stompCommandsQueue() {
    commandManager.stop = true
}

fun PlayScreen.createUnits(unitName: String, amount: Int): Array<UnitActor> {
    val units = Array<UnitActor>()

    for (i in 0 until amount) {
        units.add(unitsData.get(unitName))
    }

    return units
}

fun PlayScreen.addTurnCycleTrigger(
    turnCycles: Int,
    player: Player? = null,
    onTrigger: ((PlayScreen) -> Unit)? = null,
): Trigger {
    val trigger = TurnCycleTrigger(turnCycles, player)
    trigger.onTrigger = onTrigger
    addTrigger(trigger)

    return trigger
}

fun <T : Trigger> PlayScreen.addTrigger(trigger: T): T {
    triggerManager.addTrigger(trigger)

    return trigger
}

fun SeqAction.addOnCompleteTrigger(onTrigger: ((PlayScreen) -> Unit)? = null): Trigger {
    val trigger = OnActionCompleteTrigger(this)
    trigger.onTrigger = onTrigger
    playScreen.addTrigger(trigger)

    return trigger
}

fun Task.addOnCompleteTrigger(onTrigger: ((PlayScreen) -> Unit)? = null): Trigger {
    val trigger = OnTaskCompleteTrigger(this)
    trigger.onTrigger = onTrigger
    playScreen.addTrigger(trigger)

    return trigger
}

fun Trigger.queueCommand(command: Command) {
    onTrigger = {
        playScreen.queueCommand(command)
    }
}

fun Task.stopCommands(): Task {
    stopCommands = true
    return this
}

fun PlayScreen.queueDialogAction(sourceActor: Actor, vararg quotes: StoryDialog.Quote): SeqAction {
    return queueDialogAction(StoryDialog(sourceActor, *quotes))
}

fun PlayScreen.queueDialogAction(vararg quotes: StoryDialog.Quote): SeqAction {
    return queueDialogAction(StoryDialog(*quotes))
}

fun PlayScreen.queueDialogAction(storyDialog: StoryDialog): DialogAction {
    return queueAction(DialogAction(storyDialog))
}

fun PlayScreen.addDialogAction(storyDialog: StoryDialog): SeqAction {
    return addAction(DialogAction(storyDialog))
}

fun PlayScreen.queueTask(task: Task): Task {
    taskManager.queueTask(task)

    return task
}

fun PlayScreen.addTask(task: Task): Task {
    taskManager.addTask(task)

    return task
}

fun PlayScreen.queueCommand(command: Command): Command {
    commandManager.queueCommand(command)

    return command
}

fun PlayScreen.addCommand(command: Command) {
    commandManager.queueCommand(command)
}

fun PlayScreen.queueAddUnitAction(unitActor: UnitActor, x: Int, y: Int, cameraMove: Boolean = true): SeqAction {
    if (cameraMove)
        actionManager.queueAction(CameraMoveAction(x.unTiled(), y.unTiled()))

    val command = AddUnitAction(unitActor, x, y)
    queueAction(command)

    return command
}

fun PlayScreen.queueAddUnitAction(unitActor: UnitActor, cameraMove: Boolean = true): SeqAction {
    if (cameraMove)
        actionManager.queueAction(CameraMoveAction(unitActor.x, unitActor.y))

    val command = AddUnitAction(unitActor)
    queueAction(command)

    return command
}