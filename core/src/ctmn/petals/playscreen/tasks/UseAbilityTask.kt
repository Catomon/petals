package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.UseAbilityCommand
import ctmn.petals.playscreen.events.TaskUpdatedEvent
import ctmn.petals.unit.Ability
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.playscreen.events.CommandExecutedEvent

class UseAbilityTask(
    val ability: Ability,
    forceToComplete: Boolean = false
) : Task() {

    override var description: String? = "Use ability \"${ability.name}\""

    init {
        updateDescription()
    }

    private fun updateDescription() {
        if (ability.castAmounts > 1 && description != null) {
            val split = if (description?.contains(":") == true) description!!.split(":").first() else description

            description = "$split: ${(ability.castAmountsLeft - ability.castAmounts) * -1}/${ability.castAmounts}"
        }
    }

    private val commandListener = EventListener { event ->
        if (event is CommandExecutedEvent) {
            val command = event.command

            if (command is UseAbilityCommand && command.abilityName == ability.name) {
                updateDescription()

                playScreen.fireEvent(TaskUpdatedEvent(this@UseAbilityTask))

                if (ability.castAmountsLeft == 0)
                    isCompleted = true
            }
        }

        false
    }

    init {
        isForcePlayerToComplete = forceToComplete
    }

    override fun update(delta: Float) {
        if (isForcePlayerToComplete)
            playScreen.commandManager.getNextInQueue()?.also {
                if (it is UseAbilityCommand && it.abilityName == ability.name)
                    playScreen.commandManager.stop = false
                else
                    playScreen.commandManager.clearQueue()
            }
    }

    override fun onBegin(playScreen: PlayScreen) {
        super.onBegin(playScreen)

        playScreen.guiStage.addListener(commandListener)

        if (isForcePlayerToComplete)
            playScreen.commandManager.stop = true

        updateDescription()
    }

    override fun onCompleted() {
        super.onCompleted()

        playScreen.guiStage.removeListener(commandListener)
    }
}