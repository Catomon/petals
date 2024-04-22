package ctmn.petals.story.alissa.scenarios

import ctmn.petals.ai.SimpleBot
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.gui.widgets.StoryDialog.Quote
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.EndTurnTask
import ctmn.petals.playscreen.tasks.MoveUnitTask
import ctmn.petals.playscreen.tasks.UseAbilityTask
import ctmn.petals.playscreen.triggers.Trigger
import ctmn.petals.playscreen.triggers.TurnStartTrigger
import ctmn.petals.playstage.getLabel
import ctmn.petals.playstage.getLabels
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.*
import ctmn.petals.utils.tiledX
import ctmn.petals.utils.tiledY

class Scenario1 : AlissaScenario(name = "Slime 1", levelFileName = "level_1.map") {

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private var slime: UnitActor? = null

    init {
        player = alicePlayer
        gameEndCondition = NoEnd()
        players.add(player, enemyPlayer)

//        alice.summonAbility.castAmounts = 3
//        alice.summonAbility.castAmountsLeft = 3
    }

    override fun makeScenario(playScreen: PlayScreen) {

        // ai
        playScreen.aiManager.add(SimpleBot(enemyPlayer, playScreen))

        //labels
        for (label in playStage.getLabels()) {
            when (label.labelName) {
                "enemy_0" -> {
                    for (i in 0..2) {
                        val nSlime = SlimeLing().player(enemyPlayer)
                        playScreen.queueAddUnitAction(nSlime, label.tiledX, label.tiledY, true)

                        if (slime == null) slime = nSlime
                    }
                }
                "enemy_1" -> {

                }
                "alice" -> {
                    alice.setPosition(label.tiledX, label.tiledY + 1)
                    playScreen.queueAddUnitAction(alice, true)
                }
            }
        }

        //sequence
        playScreen {

            addAliceDiedGameOverTrigger(alice)

            val pixie = FairyPixie().player(alicePlayer)

            queueAddUnitAction(pixie, alice.tiledX - 4, alice.tiledY, true)

            queueDialogAction(
                Quote("*slurp*", slime),
                Quote("Hey", pixie),
                Quote("Hi", alice),
            ).addOnCompleteTrigger {
                pixie.remove()
            }

            queueAction {
                queueDialogAction(
                    Quote("Select Alissa and then press\n" +
                        "the marked position to move her there"))
                queueTask(MoveUnitTask(
                    alice, alice.tiledX, alice.tiledY + 2, true)
                ).description("Move Alissa to the waypoint").addOnCompleteTrigger {
                    queueDialogAction(Quote("Alissa has ability to summon faeries\n" +
                            "at a cost of her mana.\n" +
                            "You can summon up to 4 faeries at once"))
                    queueDialogAction(Quote("Summon 4 faeries and end your turn"))
                }
                queueTask(UseAbilityTask(alice.summonAbility, true).description("Summon 4 units")).addOnCompleteTrigger {
                    queueDialogAction(Quote("After you use an ability, it goes on cooldown. \n" +
                            "It will be ready after some rounds"),
                        Quote("Now press End Turn button"))
                }
                queueTask(EndTurnTask()).addOnCompleteTrigger {
                    addTrigger(TurnStartTrigger(alicePlayer)).trigger {
                        queueDialogAction(Quote("To attack, move your unit in attack range\n" +
                                "and then select an enemy unit"))
                    }
                }

                queueTask(EliminateAllEnemyUnitsTask()).addOnCompleteTrigger {
                    queueAction {
                        val taskLabel0 = playStage.getLabel("task_0")
                        queueTask(MoveUnitTask(alice, taskLabel0.tiledX, taskLabel0.tiledY)).addOnCompleteTrigger {
                            val enemyLabel1 = playStage.getLabel("enemy_1")
                            for (i in 0..4) {
                                queueAddUnitAction(SlimeLing().player(enemyPlayer), enemyLabel1.tiledX, enemyLabel1.tiledY, true)
                            }

                            queueDialogAction(StoryDialog.Quote(
                                "Units are getting terrain bonuses depending\n" +
                                        "on what terrain the tile they are on."
                            ))
                            queueDialogAction(StoryDialog.Quote(
                                "Bonuses variety for each unit and might be positive" +
                                        "\nas well as negative depending on terrain type."
                            ))

                            queueAction {
                                queueTask(EliminateAllEnemyUnitsTask()).addOnCompleteTrigger {
                                    val taskLabel1 = playStage.getLabel("task_1")
                                    queueTask(MoveUnitTask(alice, taskLabel1.tiledX, taskLabel1.tiledY).description("Move Alissa to the waypoint")).addOnCompleteTrigger {
                                        gameOverSuccess()
                                    }
                                }
                            }
                        }
                    }
                }

                addTrigger(object : Trigger() {
                    override fun check(delta: Float): Boolean {
                        for (unit in playStage.getUnitsOfPlayer(alicePlayer)) {
                            if (unit.health < 50) return true
                        }

                        return false
                    }
                }).trigger {
                    queueDialogAction(
                        Quote(
                            "Strength of a unit can drop up to -25 percent\n" +
                                    "depending on its missing health."
                        )
                    )
                }
            }
        }
    }
}
