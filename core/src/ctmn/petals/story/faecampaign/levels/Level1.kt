package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.Const.APP_NAME
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.Player
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.gui.widgets.StoryDialog
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
import ctmn.petals.unit.actors.GoblinSword

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

        result = when (playScreen.playStage.getUnitsOfPlayer(players[0]).size) {
            2 -> 3
            1 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!

        playScreen.guiStage.showCredits = false
        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))
        playScreen.fogOfWarManager.drawFog = false

        //        gameEndCondition.win()
        //                else
        //                    gameEndCondition.lose()

        val swordFaerie = playScreen.playStage.getUnitsOfPlayer(players[0]).first()

        repeat(2) {
            val swordFaerie2 = FairySword()
            playStage.addActor(swordFaerie2)
            if (it > 0)
                swordFaerie2.position(swordFaerie.tiledX + 1, swordFaerie.tiledY - 2)
            else
                swordFaerie2.position(swordFaerie.tiledX + 1, swordFaerie.tiledY + 2)
            swordFaerie2.player(players[0])
        }

        val taskUnit = swordFaerie

        val stick =
            TileActor(TileData.get("stick")!!, 10, taskUnit.tiledX + taskUnit.movingRange - 4, taskUnit.tiledY - 4)
        playStage.addActor(stick)
        playStage.addActor(
            GoblinSword().player(players[1]).position(taskUnit.tiledX + taskUnit.movingRange + 1, taskUnit.tiledY)
        )

//        val slimeLing = playStage.getUnit<SlimeLing>()!!

        playScreen {
            queueDialogAction(
                StoryDialog.Quote(
                    "Welcome to the $APP_NAME. Lets look into the basics first." +
                            "\nFollow the tasks on the top of the screen"
                )
            ).addOnCompleteTrigger {
                queueTask(
                    MoveUnitTask(
                        taskUnit,
                        taskUnit.tiledX + taskUnit.movingRange,
                        taskUnit.tiledY,
                        true
                    ).description("Select the marked unit and press on the marked tile to move to")
                ).addOnCompleteTrigger {
                    addTask(
                        ExecuteCommandTask(AttackCommand::class, true).description(
                            "While unit is selected, press on the enemy in attack range to fight"
                        )
                    ).addOnCompleteTrigger {
                        queueTask(EndTurnTask().description("Move other units and press End Turn button")).addOnCompleteTrigger {
                            val threeSlimes = Array<UnitActor>().apply {
                                playStage.getUnitsOfPlayer(players[1].id).forEach { add(it) }
                            }
                            addTask(EliminateAllEnemyUnitsTask(threeSlimes).description("Defeat all enemies"))
                        }
                    }
                }
            }

            addTrigger(UnitsDiedTrigger(playStage.getUnitsOfEnemyOf(player))).onTrigger {
                gameEndCondition.win()
            }
        }
    }
}