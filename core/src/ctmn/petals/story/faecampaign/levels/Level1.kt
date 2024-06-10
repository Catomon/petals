package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.bot.SimpleBot
import ctmn.petals.player.Player
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.EndTurnTask
import ctmn.petals.playscreen.tasks.ExecuteCommandTask
import ctmn.petals.playscreen.tasks.MoveUnitTask
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.getUnitsOfEnemyOf
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.FairySword
import ctmn.petals.unit.actors.SlimeTiny

class Level1 : Scenario("lv_1", "level_0") {

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
            Player("Slimes", 4, 4)
        )

        player = players.first()

        gameEndCondition = EliminateEnemyUnits().apply { ignorePlayers.add(players[2].id) }
    }

    override fun createLevel(playScreen: PlayScreen) {
        super.createLevel(playScreen)

        playScreen.gameMode = GameMode.STORY
    }

    override fun evaluateResult() {
        if (!gameEndCondition.winners.contains(player!!.id)) {
            result = 0
            return
        }

        result = when {
            playScreen.turnManager.round <= 14 -> 3
            playScreen.turnManager.round <= 17 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))
        playScreen.botManager.add(SimpleBot(players[2], playScreen).apply {
            simpleAI.roamingIfNoAgro = true
            simpleAI.agroRange = 3
            simpleAI.permaAgro = false
            simpleAI.roamingMaxRange = 3
        })
        playScreen.fogOfWarManager.drawFog = false

        //        gameEndCondition.win()
        //                else
        //                    gameEndCondition.lose()

        val swordFaerie = playScreen.playStage.getUnitsOfPlayer(players[0]).first()
        val swordFaerie2 = FairySword()
        playStage.addActor(swordFaerie2)
        swordFaerie2.position(swordFaerie.tiledX, swordFaerie.tiledY)
        swordFaerie2.player(players[0])
        val alice = swordFaerie

        val stick = TileActor(TileData.get("stick")!!, 10, alice.tiledX + alice.movingRange, alice.tiledY)
        playStage.addActor(stick)
        playStage.addActor(SlimeTiny().player(players[2]).position(stick.tiledX + 1, stick.tiledY))

//        val slimeLing = playStage.getUnit<SlimeLing>()!!

        playScreen {

            addTrigger(UnitsDiedTrigger(playStage.getUnitsOfEnemyOf(player))).onTrigger {
                gameEndCondition.win()
            }

            queueTask(
                MoveUnitTask(
                    alice,
                    stick.tiledX,
                    stick.tiledY,
                    true
                ).description("Select unit and press a tile to move to.")
            ).addOnCompleteTrigger {
                addTask(
                    ExecuteCommandTask(AttackCommand::class, true).description(
                        "Press a slime to fight it"
                    )
                ).addOnCompleteTrigger {
                    queueTask(EndTurnTask().description("Press End Turn button.")).addOnCompleteTrigger {
                        val threeSlimes = Array<UnitActor>().apply {
                            playStage.getUnitsOfPlayer(players[2].id).forEach { add(it) }
                        }
                        addTask(EliminateAllEnemyUnitsTask(threeSlimes).description("Kill all slimes"))
                    }
                }
            }
        }
    }
}