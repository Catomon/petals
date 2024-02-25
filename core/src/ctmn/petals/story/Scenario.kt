package ctmn.petals.story

import ctmn.petals.Assets
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.player.Player
import ctmn.petals.playscreen.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import ctmn.petals.map.loadMapFromJson
import ctmn.petals.assets as staticAssets
import ctmn.petals.playstage.PlayStage

abstract class Scenario(
    val name: String,
    val levelFileName: String,
) {

    val level by lazy {
        if (levelFileName.isNotEmpty())
            StoryLevel(Gdx.files.internal("maps/${levelFileName}").readString(), playScreen.assets)
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

    open fun saveTo(storySaveGson: StorySaveGson) {

    }

    open fun createLevel(playScreen: PlayScreen) {
        this.playScreen = playScreen
        this.playStage = playScreen.playStage

        if (level?.level != null) playScreen.setLevel(level?.level!!)
    }

    open fun makeScenario(playScreen: PlayScreen) {

    }

    open fun scenarioCreated(playScreen: PlayScreen) {

    }

    inner class StoryLevel(
        jsonString: String? = null,
        assets: Assets = staticAssets,
    ) {

        val level = if (jsonString != null) loadMapFromJson("Story", jsonString) else null

//        override fun unitParsed(json: JsonValue, unit: UnitActor): Boolean {
//            val delayed = json.extra.delayed
//            if (delayed > 0) {
//                playStage.doAtTurnsCycle(delayed) {
//                    playScreen.queueAddUnitAction(unit)
//                }
//
//                return false
//            }
//
//            val delayedTheir = json.extra.delayedTheir
//            if (json.extra.delayedTheir > 0) {
//                playStage.doAtTurnsCycle(unit.playerId, delayedTheir) {
//                    playScreen.queueAddUnitAction(unit)
//                }
//
//                return false
//            }
//
//            return super.unitParsed(json, unit)
//        }
//
//        private val JsonValue.delayed : Int get() = get("delay")?.asInt() ?: 0
//        private val JsonValue.delayedTheir : Int get() = get("delay_their")?.asInt() ?: 0
    }
}
