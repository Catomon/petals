package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.fairyUnits
import ctmn.petals.player.goblinUnits
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen
import ctmn.petals.tile.isBase
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds

class Level6 : Scenario("lv_6", "level_bases") {

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer
        )

        player = players.first()

        gameEndCondition = CaptureBases()
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
            playScreen.turnManager.round <= 15 -> 3
            playScreen.turnManager.round <= 20 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen, Array<UnitActor>().also { units ->
            goblinUnits.units.forEach {
                when (it.selfName) {
                    UnitIds.GOBLIN_SWORD,
                    UnitIds.GOBLIN_BOW,
                    UnitIds.GOBLIN_PICKAXE,
                    UnitIds.GOBLIN_SCOUT,
                    UnitIds.GOBLIN_BOAR,
                    UnitIds.GOBLIN_HEALER,
                        //UnitIds.GOBLIN_WOLF,
                        //UnitIds.goblin_machete,
                    -> units.add(it)
                }
            }
        }))
        playScreen.fogOfWarManager.drawFog = true
        playScreen.guiStage.buyMenu.availableUnits[player.id] = Array<UnitActor>().also { units ->
            fairyUnits.units.filter { unit ->
                unit.selfName == UnitIds.DOLL_SOWER
                        || unit.selfName == UnitIds.DOLL_AXE
                        || unit.selfName == UnitIds.DOLL_SWORD
                        || unit.selfName == UnitIds.DOLL_PIKE
                        || unit.selfName == UnitIds.DOLL_BOW
                        || unit.selfName == UnitIds.DOLL_HEALER
            }.forEach {
                units.add(it)
            }

            check(!units.isEmpty)
        }

        playScreen {
            queueDialogAction(StoryDialog.Quote("To unlock certain units, you need to\n" +
                    "order a Fairy Sower to create the structures specified in the base menu under each unit."))

            addTurnCycleTrigger(1, players[0]).onTrigger {
                queueDialogAction(StoryDialog.Quote("Enemy bases can be destroyed by most ground units"))
                queueDialogAction(StoryDialog.Quote("Get your units on top of enemy base and destroy it")).addOnCompleteTrigger {
                    queueTask(object : Task() {
                        override var description: String? = "Destroy all enemy bases"

                        override fun update(delta: Float) {
                            if (playStage.getCapturablesOf(players[1]).none { it.isBase })
                                complete()
                        }
                    })
                }
            }
        }
    }
}