package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.bot.SimpleBot
import ctmn.petals.player.*
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.actors.creatures.BigEvilTree
import ctmn.petals.unit.cLeader
import ctmn.petals.unit.leader

class LevelTree2 : Scenario(ID, "level_tree_2") {

    companion object {
        const val ID = "tree_2"
    }

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
            Player("Slimes", 4, 4)
        )

        player = players.first()

        gameEndCondition = CaptureBases().apply { ignorePlayers.add(players[2].id) }
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

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen, Array<UnitActor>().also { units ->
            goblinUnits.units.forEach {
                when (it.selfName) {
                    UnitIds.GOBLIN_SWORD,
                    UnitIds.GOBLIN_SPEAR,
                    UnitIds.GOBLIN_BOW,
                    UnitIds.GOBLIN_PICKAXE,
                    UnitIds.GOBLIN_SCOUT,
                    UnitIds.GOBLIN_BOAR,
                    UnitIds.GOBLIN_HEALER,
                    UnitIds.GOBLIN_WOLF,
                    UnitIds.GOBLIN_MACHETE,
                    UnitIds.GOBLIN_WYVERN,
                    UnitIds.GOBLIN_CROSSBOW,
                    UnitIds.GOBLIN_CATAPULT,
                    -> units.add(it)
                }
            }
        }))
        playScreen.botManager.add(SimpleBot(players[2], playScreen).apply {
            simpleAI.roamingIfNoAgro = true
            simpleAI.agroRange = 1
            simpleAI.permaAgro = false
            simpleAI.roamingMaxRange = 3
        })

        playScreen.fogOfWarManager.drawFog = true
        playScreen.guiStage.buyMenu.availableUnits[player.id] = Array<UnitActor>().also { units ->
            fairyUnits.units.filter { unit ->
                unit.selfName == UnitIds.DOLL_SOWER
                        || unit.selfName == UnitIds.DOLL_AXE
                        || unit.selfName == UnitIds.DOLL_SWORD
                        || unit.selfName == UnitIds.DOLL_PIKE
                        || unit.selfName == UnitIds.DOLL_BOW
                        || unit.selfName == UnitIds.DOLL_SCOUT
                        || unit.selfName == UnitIds.DOLL_HEALER
                        || unit.selfName == UnitIds.DOLL_SHIELD
                        || unit.selfName == UnitIds.PIXIE
                        || unit.selfName == UnitIds.DOLL_BOMBER
            }.forEach {
                units.add(it)
            }

            check(!units.isEmpty)
        }

        playScreen {
            playStage.getUnitsOfPlayer(players[2]).forEach {
                if (it is BigEvilTree) {
                    it.leader(3, 2, false)
                    it.cLeader?.leaderDmgBuff = 15
                }
            }
        }
    }
}