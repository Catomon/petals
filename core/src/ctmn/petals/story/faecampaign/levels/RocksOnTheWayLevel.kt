package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.SimpleBot
import ctmn.petals.player.Player
import ctmn.petals.player.fairyUnits
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.seqactions.SeqAction
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.playscreen.tasks.KillUnitTask
import ctmn.petals.playscreen.tasks.MoveAnyUnitTask
import ctmn.petals.playscreen.triggers.PlayerHasNoUnits
import ctmn.petals.playscreen.triggers.UnitAddedTrigger
import ctmn.petals.playstage.getUnits
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.gameOverFailure
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.playerId
import ctmn.petals.unit.teamId

class RocksOnTheWayLevel : Scenario(ID, "Rocks_in_the_way") {

    companion object {
        const val ID = "rocks_on_the_way"
    }


    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
            Player("Creatures", 4, 4)
        )

        player = players.first()

        gameEndCondition = Manual()
    }

    override fun createLevel(playScreen: PlayScreen) {
        super.createLevel(playScreen)

        playScreen.gameMode = GameMode.CRYSTALS
    }

    override fun evaluateResult() {
        if (!gameEndCondition.winners.contains(player!!.id)) {
            result = 0
            return
        }

        result = when {
            playScreen.turnManager.round <= 20 -> 3
            playScreen.turnManager.round <= 25 -> 2
            else -> 1
        }
    }

    private var unitFairyHammerMade = false
    private var maceGoblin: UnitActor? = null

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!
        player.creditsPassiveReserve = 0

        playScreen.botManager.add(SimpleBot(players[1], playScreen).apply {
            simpleAI.roamingIfNoAgro = true
            simpleAI.agroRange = 2
            simpleAI.permaAgro = false
            simpleAI.roamingMaxRange = 2
        })
        playScreen.botManager.add(SimpleBot(players[2], playScreen).apply {
            simpleAI.roamingIfNoAgro = true
            simpleAI.agroRange = 2
            simpleAI.permaAgro = false
            simpleAI.roamingMaxRange = 2
        })

        playScreen.fogOfWarManager.drawFog = true
        playScreen.fogOfWarManager.drawDiscoverableFog = true
        playScreen.guiStage.buyMenu.availableUnits[player.id] = Array<UnitActor>().also { units ->
            fairyUnits.units.filter { unit ->
                unit.selfName == UnitIds.DOLL_HAMMER
                        || unit.selfName == UnitIds.DOLL_SOWER
            }.forEach {
                units.add(it)
            }

            check(!units.isEmpty)
        }

        playScreen {
            playStage.getUnits().forEach { if (it.playerId < 0) it.teamId = 4 }

            maceGoblin = playStage.getUnitsOfPlayer(players[1]).first { it.selfName == UnitIds.GOBLIN_MACE }
            maceGoblin?.remove()

            queueDialogAction(
                StoryDialog.Quote(
                    "Looks like rocks are blocking your way.\n" +
                            "In order to break them, you need Hammer Fairy."
                )
            )
            queueDialogAction(
                StoryDialog.Quote(
                    "Look around for some crystal shards\n" +
                            "to make a base and create Hammer Fairy."
                )
            )

            addTrigger(PlayerHasNoUnits(players[0])).onTrigger {
                gameOverFailure()
            }

            addTrigger(UnitAddedTrigger(players[0].id)).onTrigger {
                if (playStage.getUnitsOfPlayer(players[0].id).any { it.selfName == UnitIds.DOLL_HAMMER }) {
                    if (!unitFairyHammerMade) {
                        queueAddUnitAction(maceGoblin ?: throw IllegalStateException("Where's mace goblin???"), true)

                        queueDialogAction(StoryDialog.Quote("Out of my way!", maceGoblin))

                        queueTask(KillUnitTask(maceGoblin!!)).addOnCompleteTrigger {
                            queueTask(MoveAnyUnitTask(players[0].id, 18, 4)).addOnCompleteTrigger {
                                gameOverSuccess()
                            }
                        }

                        unitFairyHammerMade = true
                    }
                }
            }.dontRemoveOnTrigger()
        }
    }
}