package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.assets
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.bot.SimpleBot
import ctmn.petals.player.*
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.gui.widgets.said
import ctmn.petals.playscreen.listeners.TurnsCycleListener
import ctmn.petals.playscreen.tasks.*
import ctmn.petals.playscreen.triggers.*
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getUnit
import ctmn.petals.playstage.getUnitsOfEnemyOf
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.*
import ctmn.petals.story.alissa.CreateUnit.alice
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.unit.*
import ctmn.petals.unit.abilities.HealthPotionAbility
import ctmn.petals.unit.actors.FairyAxe
import ctmn.petals.unit.actors.SlimeLing
import ctmn.petals.unit.actors.SlimeTiny

class Level1 : Scenario("lv_1", "alice_slime2") {

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
            Player("Slimes", 4, 4)
        )

        player = players.first()

        gameEndCondition = Manual().apply { ignorePlayers.add(players[2].id) }
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

        val alice = playScreen.alice()
        val stick = TileActor(TileData.get("stick")!!, 10, alice.tiledX + alice.movingRange, alice.tiledY)
        playStage.addActor(stick)
        playStage.addActor(SlimeTiny().player(players[2]).position(stick.tiledX + 1, stick.tiledY))

        val slimeLing = playStage.getUnit<SlimeLing>()!!
        slimeLing.item = assets.tilesAtlas.findRegion("items/book")

        playScreen {
//            queueDialogAction(
//                StoryDialog(
//                    "Alice story." said null,
//                    "Look I spot a good stick over there!" said alice,
//                )
//            )

            addAliceDiedGameOverTrigger()

            addTrigger(UnitsDiedTrigger(playStage.getUnitsOfEnemyOf(player))).onTrigger {
                gameEndCondition.win()
            }

            addTrigger(object : Trigger() {
                override fun check(delta: Float): Boolean = alice.health <= 50 && turnManager.currentPlayer == player
            }).onTrigger {
                alice.abilities.add(HealthPotionAbility().also { it.level = 1; it.cooldown = 3 })
                queueTask(
                    UseAbilityTask(alice.abilities.first(), true).description("Use an ability to heal up Alice")
                )
            }

            queueTask(
                MoveUnitTask(
                    alice,
                    stick.tiledX,
                    stick.tiledY,
                    true
                ).description("Select Alice and press a tile to move to.")
            ).addOnCompleteTrigger {
                stick.remove()
                alice.attackAnimation = alice.stickAttackAni
                alice.setAnimation(alice.pickUpAni)
                alice.addAttackDamage(10)
                queueTask(
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

            addTrigger(UnitsDiedTrigger(slimeLing)).onTrigger {
//                queueDialogAction(
//                    StoryDialog.Quote("book", alice),
//                )
            }
        }
    }
}