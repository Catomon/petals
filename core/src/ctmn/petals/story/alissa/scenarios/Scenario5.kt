package ctmn.petals.story.alissa.scenarios

import ctmn.petals.actors.actions.JumpAction
import ctmn.petals.ai.SimpleBot
import ctmn.petals.map.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.seqactions.ActorAction
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.seqactions.DialogAction
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.triggers.TurnCycleTrigger
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.getLabels
import ctmn.petals.playstage.tiledHeight
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.*
import ctmn.petals.utils.tileCenterX
import ctmn.petals.utils.tileCenterY

class Scenario5 : AlissaScenario(name = "Goblins 2", levelFileName = "level_5.map") {

    private val allyPlayer = alicePlayer

    //Player("Ally", Player.GREEN, Team.GREEN).apply {
    //        allies.add(alicePlayer.teamId)
    //    }

    private val goblinsPlayer = Player("Goblins", Player.RED, Team.RED)

    private val slimesPlayer = Player("Slimes", Player.YELLOW, Team.YELLOW)

    private val cherie = Cherie()
    private val enemyLeader = GoblinLeader()

    private lateinit var label0: LabelActor
    private lateinit var label1: LabelActor
    private lateinit var label2: LabelActor
    private lateinit var hugeSlimeLabel: LabelActor

    private val slimeHuge = SlimeHuge()

    init {
        alicePlayer.allies.add(allyPlayer.teamId)
        alice.player(alicePlayer)

        player = alicePlayer
        gameEndCondition = NoEnd()

        players.add(player, /* allyPlayer, */ goblinsPlayer, slimesPlayer)

        unitsToSaveProgressOf.add(cherie)
    }

    override fun makeScenario(playScreen: PlayScreen) {

        // AI
        //playScreen.botManager.add(EasyDuelBot(allyPlayer, playScreen))
        playScreen.botManager.add(SimpleBot(goblinsPlayer, playScreen))
        playScreen.botManager.add(SimpleBot(slimesPlayer, playScreen))

        //labels
        for (label in playStage.getLabels()) {
            when (label.labelName) {
                "alice" -> {
                    alice.position(label)
                    playScreen.queueAddUnitAction(alice)
                }
                "ally" -> {
                    playScreen.queueAddUnitAction(cherie.player(allyPlayer).leader(2, 2, true).position(label))
                    for (i in 0..3) {
                        playScreen.queueAddUnitAction(CherieSpearman().player(allyPlayer).followerOf(2).position(label))
                    }
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
                "enemy_3" -> {
                    hugeSlimeLabel = label
                }
            }
        }

        //sequence
        playScreen {
            addAliceDiedGameOverTrigger(alice)

            addTrigger(UnitsDiedTrigger(cherie)).trigger {
                actionManager.queueFirst(
                    DialogAction(
                        StoryDialog(
                            StoryDialog.Quote("They got me", cherie),
                            StoryDialog.Quote("Stay back, I'll handle them", alice),
                        )
                    )
                )
            }

            queueDialogAction(
                StoryDialog.Quote("Quote", cherie),
                StoryDialog.Quote("Quote", alice),
                )

            //label0
            queueAddUnitAction(enemyLeader.player(goblinsPlayer).leader(3, 2, true).position(label0))
            for (i in 0..2) {
                queueAddUnitAction(GoblinBoar().player(goblinsPlayer).followerOf(3).position(label0))
            }
            //label1 goblins that are behind
            queueAddUnitAction(GoblinLeader().player(goblinsPlayer).leader(4, 2, true).position(label1))
            for (i in 0..2) {
                queueAddUnitAction(GoblinSword().player(goblinsPlayer).followerOf(4).position(label1))
            }
            //label2
            queueAddUnitAction(GoblinLeader().player(goblinsPlayer).leader(5, 2, true).position(label2))
            for (i in 0..3) {
                queueAddUnitAction(GoblinBoar().player(goblinsPlayer).followerOf(5).position(label2))
            }

            //label3
            //slime appearance
            addTrigger(TurnCycleTrigger(2, slimesPlayer)).trigger {
                slimeAppearance()
            }

            queueDialogAction(
                StoryDialog.Quote("dfgdfgdfg", enemyLeader),
            )

            queueDialogAction(
                StoryDialog.Quote("Quote", cherie),
                StoryDialog.Quote("Quote", cherie),
            )

            queueAction {
                queueTask(EliminateAllEnemyUnitsTask()).addOnCompleteTrigger {
                    queueDialogAction(
                        StoryDialog.Quote("", cherie),
                        StoryDialog.Quote("", alice),
                    )

                    queueAction {
                        gameOverSuccess()
                    }
                }
            }
        }
    }

    private fun PlayScreen.slimeAppearance() {
        queueAddUnitAction(slimeHuge.player(slimesPlayer).position(0, playStage.tiledHeight()).leader(6, 1, true), false).addOnCompleteTrigger {
            actionManager.queueAction(CameraMoveAction(hugeSlimeLabel.tileCenterX, hugeSlimeLabel.tileCenterY))
                .addOnCompleteTrigger {
                    addAction(ActorAction(slimeHuge, JumpAction(hugeSlimeLabel.tileCenterX, hugeSlimeLabel.tileCenterY)))
                        .addOnCompleteTrigger {

                            queueDialogAction(
                                StoryDialog.Quote("Quote", alice),
                                StoryDialog.Quote("Quote", cherie),
                            )
                        }
                }
        }
    }
}