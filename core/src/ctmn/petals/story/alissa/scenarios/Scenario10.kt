package ctmn.petals.story.alissa.scenarios

import ctmn.petals.ai.SimpleBot
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.tasks.MoveUnitTask
import ctmn.petals.playscreen.triggers.PlayerHasNoUnits
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getLabel
import ctmn.petals.playstage.getLabels
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.*
import ctmn.petals.utils.addUnit
import ctmn.petals.utils.tiledX
import ctmn.petals.utils.tiledY

class Scenario10 : AlissaScenario("Swamp 2", "level_10.map") {

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private val cherie = Cherie()

    private val allyGoblin = Goblin().player(alicePlayer)

    init {
        player = alicePlayer
        gameEndCondition = NoEnd()

        players.add(player, enemyPlayer)

        unitsToSaveProgressOf.add(cherie)
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        //
        playStage.timeOfDay = PlayStage.DayTime.NIGHT

        playScreen.fogOfWarManager.drawFog = true
        playScreen.fogOfWarManager.drawDiscoverableFog = true

        // ai
        playScreen.botManager.add(SimpleBot(enemyPlayer, playScreen).apply {
            simpleAI.permaAgro = false
            simpleAI.roamingIfNoAgro = true
            simpleAI.agroRange = 6
        })

        // labels
        playStage.getLabels().forEach { label ->
            when (label.labelName) {
                "alice" -> label.addUnit(alice)

                "ally" -> {
                    label.addUnit(cherie.player(alicePlayer).leader(2, 2, true))
                    for (i in 0..3) {
                        label.addUnit(CherieSpearman().player(alicePlayer).followerOf(2))
                    }
                }
                "ally_1" -> {
                    label.addUnit(allyGoblin)
                }

                "pink_0" -> {
                    label.addUnit(PinkSlimeLing().player(enemyPlayer))
                }
                "pink_1" -> {
                    label.addUnit(PinkSlimeLing().player(enemyPlayer))
                }

                "enemy_0" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_1" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_2" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_3" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_4" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_5" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }

                "enemy_6" -> {
                    label.addUnit(Slime().player(enemyPlayer).leader(3, 2, true).position(label))
                    for (i in 0..3) {
                        label.addUnit(SlimeLing().player(enemyPlayer).followerOf(3).position(label))
                    }
                }

                "enemy_7" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_8" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }

                "enemy_9" -> {
                    label.addUnit(SlimeHuge().player(enemyPlayer).leader(4, 2, true).position(label))
                }
                "enemy_10" -> {
                    label.addUnit(SlimeHuge().player(enemyPlayer).leader(5, 2, true).position(label))
                }
            }
        }

        playScreen {
            addAliceDiedGameOverTrigger()

            val taskLabel = playStage.getLabel("task")
            queueTask(MoveUnitTask(alice, taskLabel.tiledX, taskLabel.tiledY).description("Reach the marked position")).addOnCompleteTrigger {
                gameOverSuccess()
            }

            queueDialogAction(
                StoryDialog.Quote("sdfsdf!", allyGoblin),
                StoryDialog.Quote("sdfsdf!\n" +
                        "sdfsdf!", cherie),
                StoryDialog.Quote("My bad.", allyGoblin),
            )

            queueAction {
                addTrigger(PlayerHasNoUnits(enemyPlayer).trigger {
                    queueDialogAction(
                        StoryDialog.Quote("Don't see any more enemies here,\n" +
                                "lets move on", cherie)
                    )

                    queueAction {
                        gameOverSuccess()
                    }
                })
            }
        }
    }
}