package ctmn.petals.story.alissa.scenarios

import ctmn.petals.bot.SimpleBot
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.getLabels
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverFailure
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.*

class Scenario4 : AlissaScenario(name = "Goblins", levelFileName = "level_4.map") {

    private val allyPlayer = Player("Ally", Player.GREEN, Team.GREEN).apply {
        allies.add(alicePlayer.teamId)
    }

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private val cherie = Cherie()
    private val enemyLeader = GoblinLeader()

    init {
        alicePlayer.allies.add(allyPlayer.teamId)
        alice.player(alicePlayer)

        player = alicePlayer
        gameEndCondition = NoEnd()

        players.add(player, enemyPlayer, allyPlayer)

        unitsToSaveProgressOf.add(cherie)
    }

    override fun makeScenario(playScreen: PlayScreen) {

        // ai
        playScreen.botManager.add(SimpleBot(enemyPlayer, playScreen))
        playScreen.botManager.add(SimpleBot(allyPlayer, playScreen))

        //labels
        for (label in playStage.getLabels()) {
            when (label.labelName) {
                "alice" -> {
                    alice.position(label)
                }
                "ally" -> {
                    playScreen.queueAddUnitAction(cherie.player(allyPlayer).leader(2, 2, true).position(label))
                    for (i in 0..3) {
                        playScreen.queueAddUnitAction(CherieSpearman().player(allyPlayer).followerOf(2).position(label))
                    }
                }
                "enemy_0" -> {
                    playScreen.queueAddUnitAction(enemyLeader.player(enemyPlayer).leader(3, 2, false).position(label))
                    for (i in 0..3) {
                        playScreen.queueAddUnitAction(GoblinSword().player(enemyPlayer).followerOf(3).position(label))
                    }
                }
                "enemy_1" -> {
                    playScreen.queueAddUnitAction(GoblinLeader().player(enemyPlayer).leader(4, 2, false).position(label))
                    for (i in 0..3) {
                        playScreen.queueAddUnitAction(GoblinSword().player(enemyPlayer).followerOf(4).position(label))
                    }
                }
            }
        }

        //sequence
        playScreen {
            addAliceDiedGameOverTrigger(alice)

            addTrigger(UnitsDiedTrigger(cherie)).onTrigger {
                gameOverFailure()
            }

            queueDialogAction(
                StoryDialog.Quote("fddfgdfgdf", enemyLeader),
            )

            queueAddUnitAction(alice)

            queueAction {
                queueTask(EliminateAllEnemyUnitsTask()).addOnCompleteTrigger {
                    queueDialogAction(
                        StoryDialog.Quote("Quote", cherie),
                        StoryDialog.Quote("Quote", alice),

                    )

                    queueAction {
                        gameOverSuccess()
                    }
                }
            }
        }
    }
}