package ctmn.petals.story

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.player.Player
import ctmn.petals.playscreen.*
import com.badlogic.gdx.utils.Array
import ctmn.petals.map.loadMap
import ctmn.petals.map.loadScenarioMap
import ctmn.petals.playstage.PlayStage
import ctmn.petals.utils.log

abstract class Scenario(
    val id: String,
    val mapFileName: String,
) {

    var result = 0

    val map by lazy {
        if (mapFileName.isNotEmpty())
            loadScenarioMap(mapFileName)
        else
            null
    }
    var player: Player? = null
    var gameEndCondition: GameEndCondition = NoEnd()
    val players: Array<Player> = Array()

    lateinit var playScreen: PlayScreen
    lateinit var playStage: PlayStage

    open fun loadFrom(storySaveGson: StorySaveGson) {

    }

    open fun evaluateResult() {
        result = 1
    }

    open fun saveTo(storySaveGson: StorySaveGson) {
        evaluateResult()
        log("Result: $result")
        val lp = storySaveGson.progress.levels[id]
        if (lp != null) {
            if (lp.state < result)
                lp.state = result
        } else {
            storySaveGson.progress.levels[id] = LevelProgress(result)
        }
    }

    open fun createLevel(playScreen: PlayScreen) {
        this.playScreen = playScreen
        this.playStage = playScreen.playStage

        if (map != null) playScreen.setLevel(map!!)
    }

    open fun makeScenario(playScreen: PlayScreen) {

    }

    open fun scenarioCreated(playScreen: PlayScreen) {

    }
}
