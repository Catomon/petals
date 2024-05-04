package ctmn.petals.story.alissa.scenarios

import ctmn.petals.bot.SimpleBot
import ctmn.petals.map.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog.Quote
import ctmn.petals.playscreen.tasks.KillUnitTask
import ctmn.petals.playscreen.tasks.MoveUnitTask
import ctmn.petals.playscreen.triggers.PlayerHasNoUnits
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getLabels
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.BigEvilTree
import ctmn.petals.unit.actors.Cherie
import ctmn.petals.unit.actors.CherieSpearman
import ctmn.petals.unit.actors.EvilTree
import ctmn.petals.utils.addUnit
import ctmn.petals.utils.tiledX
import ctmn.petals.utils.tiledY

class Scenario8 : AlissaScenario("Evil Trees 3", "level_8.map") {

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private val bigEvilTree = BigEvilTree().player(enemyPlayer).apply {
        leader(evilTreeLeaderId, 2, true)
        cLeader!!.leaderDefBuff = 25
    }

    private val evilTreeLeaderId = 9423

    private lateinit var bigEvilTreeLabel: LabelActor

    private lateinit var evilTreeLabel3: LabelActor
    private lateinit var evilTreeLabel4: LabelActor
    private lateinit var evilTreeLabel5: LabelActor

    private lateinit var waypoint2: LabelActor

    private val cherie = Cherie()

    init {
        player = alicePlayer
        gameEndCondition = NoEnd()

        players.add(player, enemyPlayer)

        unitsToSaveProgressOf.add(cherie)
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        playScreen.fogOfWarManager.drawFog = true

        // ai
        playScreen.botManager.add(SimpleBot(enemyPlayer, playScreen).apply {
            simpleAI.permaAgro = false
            simpleAI.roamingIfNoAgro = false
            simpleAI.agroRange = 8
        })

        // labels
        playStage.getLabels().forEach { label ->
            when (label.labelName) {
                "alice" -> label.addUnit(alice)

                "ally" -> {
                    label.addUnit(cherie.player(alicePlayer).leader(2, 2, true).position(label))
                    for (i in 0..3) {
                        label.addUnit(CherieSpearman().player(alicePlayer).followerOf(2).position(label))
                    }
                }

                "tree1" -> label.addUnit(EvilTree().player(enemyPlayer))
                "tree2" -> label.addUnit(EvilTree().player(enemyPlayer))

                "tree3" -> evilTreeLabel3 = label
                "tree4" -> evilTreeLabel4 = label
                "tree5" -> evilTreeLabel5 = label

                "tree" -> bigEvilTreeLabel = label

                "waypoint" -> waypoint2 = label
            }
        }

        playScreen {
            playStage.timeOfDay = PlayStage.DayTime.EVENING

            addAliceDiedGameOverTrigger()

            queueDialogAction(Quote("Ok", alice))
            queueDialogAction(Quote("Ok", cherie))

            addTrigger(PlayerHasNoUnits(enemyPlayer)).trigger {

            }

            queueTask(MoveUnitTask(alice, bigEvilTreeLabel.tiledX, bigEvilTreeLabel.tiledY - 6).description("Bring Alice to waypoint")).addOnCompleteTrigger {

                queueAddUnitAction(bigEvilTree.position(bigEvilTreeLabel))
                queueDialogAction(
                    Quote("Ok", bigEvilTree),
                    Quote("Ok", alice),
                )

                queueAddUnitAction(EvilTree().player(enemyPlayer).followerOf(evilTreeLeaderId).position(evilTreeLabel3))
                queueAddUnitAction(EvilTree().player(enemyPlayer).followerOf(evilTreeLeaderId).position(evilTreeLabel3))

                queueAddUnitAction(EvilTree().player(enemyPlayer).followerOf(evilTreeLeaderId).position(evilTreeLabel4))
                queueAddUnitAction(EvilTree().player(enemyPlayer).followerOf(evilTreeLeaderId).position(evilTreeLabel4))

                queueAddUnitAction(EvilTree().player(enemyPlayer).followerOf(evilTreeLeaderId).position(evilTreeLabel5))
                queueAddUnitAction(EvilTree().player(enemyPlayer).followerOf(evilTreeLeaderId).position(evilTreeLabel3))

                queueAction {
                    addTask(MoveUnitTask(alice, waypoint2.tiledX, waypoint2.tiledY).description("Bring Alice to waypoint")).addOnCompleteTrigger {
                        queueDialogAction(
                            Quote("Coward!", bigEvilTree),
                            Quote("Nah uh", alice)
                        )

                        queueAction {
                            gameOverSuccess()
                        }
                    }

                    addTask(KillUnitTask(bigEvilTree).description("OR defeat the Evil Tree")).addOnCompleteTrigger {
                        bigEvilTree.remove()

                        val tree = BigEvilTree().position(bigEvilTreeLabel)

                        queueAddUnitAction(tree, false)

                        queueDialogAction(
                            Quote("Good job.", tree),
                            Quote("Ok", alice)
                        )

                        queueAction {
                            gameOverSuccess()
                        }
                    }
                }
            }
        }
    }
}