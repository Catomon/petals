package ctmn.petals.bot.mediocre

import ctmn.petals.bot.BotAction
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.UseAbilityCommand
import ctmn.petals.playstage.getUnits
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.*
import ctmn.petals.unit.abilities.HealingTouchAbility
import ctmn.petals.unit.abilities.HealthPotionAbility
import ctmn.petals.unit.actors.creatures.BunnySlimeHuge
import ctmn.petals.utils.getSurroundingTiles
import ctmn.petals.utils.logErr
import ctmn.petals.utils.tiledX

class UseAbilityAction(private val unit: UnitActor,
    private val bot: MidBot, val playScreen: PlayScreen,
) : BotAction() {

    private val player = bot.player

    private var command: UseAbilityCommand? = null

    override fun evaluate(): Int {
        priority = defaultPriority

        if (unit.cAbilities?.abilities != null && !unit.cAbilities!!.abilities.isEmpty()) {
            val ability = unit.cAbilities!!.abilities.first()

            when (ability) {
                is BunnySlimeHuge.SlimeJumpAbility -> {
                    var freeTile: TileActor? = null
                    var enemyUnit: UnitActor? = playScreen.playStage.getUnits().apply {
                        removeAll {
                            it == unit
                                    || it.isAlly(player)
                        }
                    }.firstOrNull {
                        freeTile = playScreen.playStage.getSurroundingTiles(it.tiledX, it.tiledY, true)
                            .firstOrNull { tile -> unit.isInRange(tile.tiledX, tile.tiledY, ability.range) }
                        freeTile != null
                    } ?: return IMPOSSIBLE

                    if (freeTile == null) return IMPOSSIBLE

                    val command = UseAbilityCommand(
                        unit,
                        ability,
                        freeTile!!.tiledX,
                        freeTile!!.tiledY
                    )

                    if (command.canExecute(playScreen)) {
                        this.command = command
                        return priority
                    }
                }

                is HealthPotionAbility -> {
                    val teamUnit: UnitActor = playScreen.playStage.getUnits().filter {
                        it.isInRange(unit.tiledX, unit.tiledY, ability.range)
                    }.sortedBy { it.health }.firstOrNull { it.health < it.cUnit.baseHealth } ?: return IMPOSSIBLE

                    val command = UseAbilityCommand(
                        unit,
                        ability,
                        teamUnit.tiledX,
                        teamUnit.tiledY
                    )

                    if (command.canExecute(playScreen)) {
                        this.command = command
                        return priority
                    }
                }

                is HealingTouchAbility -> {
                    val teamUnit: UnitActor = playScreen.playStage.getUnits().filter {
                        it.isInRange(unit.tiledX, unit.tiledY, ability.range)
                    }.sortedBy { it.health }.firstOrNull { it.health < it.cUnit.baseHealth } ?: return IMPOSSIBLE

                    val command = UseAbilityCommand(
                        unit,
                        ability,
                        teamUnit.tiledX,
                        teamUnit.tiledY
                    )

                    if (command.canExecute(playScreen)) {
                        this.command = command
                        return priority
                    }
                }
            }
        }

        return IMPOSSIBLE
    }

    override fun execute(): Boolean {
        val command = command
        if (command != null) {
            return playScreen.commandManager.queueCommand(command)
        } else {
            logErr("execute() failed.")
        }

        return false
    }
}