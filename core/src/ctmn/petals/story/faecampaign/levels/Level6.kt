package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.SpeciesUnitNotFoundExc
import ctmn.petals.player.fairyUnits
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.listeners.TurnsCycleListener
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.KeepPlayerUnitsAlive
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.playscreen.triggers.PlayerHasNoUnits
import ctmn.petals.playscreen.triggers.TurnCycleTrigger
import ctmn.petals.playscreen.triggers.UnitPosRectTrigger
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getUnit
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.actors.FairyAxe
import ctmn.petals.unit.isAlly
import ctmn.petals.unit.tiledY

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

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))
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
            queueTask(object : Task() {
                override var description: String? = "Capture all enemy bases"

                override fun update(delta: Float) {
                    if (playStage.getCapturablesOf(players[1])
                            .none { playStage.getUnit(it.tiledX, it.tiledY)?.isAlly(player) != true }
                    )
                        complete()
                }
            })
        }
    }
}