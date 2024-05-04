package ctmn.petals.story.alissa.scenarios

import ctmn.petals.bot.SimpleBot
import ctmn.petals.map.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getLabels
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.*
import ctmn.petals.utils.addUnit

class Scenario11 : AlissaScenario("Goblins 3", "level_11.map") {

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private val cherie = Cherie()

    private val allyGoblin = Goblin().player(alicePlayer)

    private lateinit var label0: LabelActor
    private lateinit var label1: LabelActor
    private lateinit var label2: LabelActor

    private val enemyLeader = GoblinLeader()

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

        // ai
        playScreen.botManager.add(SimpleBot(enemyPlayer, playScreen))

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

                "enemy_0" -> {
                    label0 = label
                }
                "enemy_1" -> {
                    label1 = label
                }
                "enemy_2" -> {
                    label2 = label
                }
            }
        }

        playScreen {
            addAliceDiedGameOverTrigger()

            queueDialogAction(
                StoryDialog.Quote("xd", allyGoblin),
                StoryDialog.Quote("..", allyGoblin),
            )

            queueAction {
                allyGoblin.remove()
            }

            queueAddUnitAction(GoblinLeader().player(enemyPlayer).leader(3, 2, true).position(label0))
            for (i in 0..3) {
                queueAddUnitAction(GoblinBoar().player(enemyPlayer).followerOf(3).position(label0))
            }

            queueAddUnitAction(enemyLeader.player(enemyPlayer).leader(4, 2, true).position(label1))
            for (i in 0..5) {
                queueAddUnitAction(GoblinSword().player(enemyPlayer).followerOf(4).position(label1))
            }

            queueAddUnitAction(GoblinLeader().player(enemyPlayer).leader(5, 2, true).position(label2))
            for (i in 0..3) {
                queueAddUnitAction(GoblinBow().player(enemyPlayer).followerOf(5).position(label2))
            }

            queueDialogAction(
                StoryDialog.Quote("Now we got u, like fr fr!", enemyLeader),
            )

            queueAction {
                queueTask(EliminateAllEnemyUnitsTask().description("Defeat the goblins")).addOnCompleteTrigger {
                    queueDialogAction(
                        StoryDialog.Quote("Don't see any more enemies here,\n" +
                                "lets move on", cherie)
                    )

                    queueAction {
                        gameOverSuccess()
                    }
                }
            }
        }
    }
}