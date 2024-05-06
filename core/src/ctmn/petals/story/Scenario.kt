package ctmn.petals.story

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.player.Player
import ctmn.petals.playscreen.*
import com.badlogic.gdx.utils.Array
import ctmn.petals.map.loadMap
import ctmn.petals.playstage.PlayStage

abstract class Scenario(
    val id: String,
    val mapFileName: String,
) {

    var result = 1

    val map by lazy {
        if (mapFileName.isNotEmpty())
            loadMap(mapFileName)
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

    }

    open fun saveTo(storySaveGson: StorySaveGson) {
        evaluateResult()
        storySaveGson.progress.levels.put(id, LevelProgress(result))
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
