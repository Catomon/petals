package ctmn.petals.story.levels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.events.GameOverEvent
import ctmn.petals.playscreen.events.UnitDiedEvent
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.playerId
import ctmn.petals.utils.log

class Level1 : Scenario("Level 1", "level_1") {

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
        )

        player = players.first()

        gameEndCondition = EliminateEnemyUnits()
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))

        playScreen {
            addTask(EliminateAllEnemyUnitsTask().description("Kill enemy units")).addOnCompleteTrigger {
                //gameOverSuccess()
            }

            addTask(object : Task() {

                init {
                    description = "Keep your units alive"
                }

                val playStageL = object : EventListener {
                    override fun handle(event: Event?): Boolean {
                        if (event is ActionCompletedEvent) {
                            enemyDown = playStage.getUnitsOfPlayer(players[1]).size == 0
                        }

                        if (event is UnitDiedEvent) {
                            if (event.unit.playerId == 1) {
                                complete(state = State.FAILED)
                                log("task failed")
                            }
                        }

                        if (event is GameOverEvent) {
                            if (state != State.FAILED)
                                complete()
                        }

                        return false
                    }
                }
                var enemyDown = false

                override fun update(delta: Float) {
                    super.update(delta)

                    if (!isCompleted) {
                        if (isGameOver && enemyDown && state != State.FAILED)
                            complete()
                    }
                }

                override fun onBegin(playScreen: PlayScreen) {
                    super.onBegin(playScreen)

                    playStage.addListener(playStageL)
                }

                override fun onCompleted() {
                    super.onCompleted()

                    playStage.removeListener(playStageL)
                }
            })
        }
    }
}