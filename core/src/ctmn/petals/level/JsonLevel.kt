package ctmn.petals.level

import ctmn.petals.Assets
import ctmn.petals.gameactors.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.unit.*
import ctmn.petals.unit.component.FollowerComponent
import ctmn.petals.unit.component.LeaderComponent
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ArrayMap
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import ctmn.petals.tile.setTileCrystalPlayer
import ctmn.petals.unit.UnitActor

/** Usage: JsonLevel.fromFile(name).initActors(assets) */
open class JsonLevel private constructor() : Level {

    lateinit var name: String

    var fileName: String = ""

    var gameMode: String = ""

    lateinit var jsonActors: JsonValue

    override val players: Array<Player> = Array()
    override val tiles: Array<TileActor> = Array()
    override val units: Array<UnitActor> = Array()
    override val labels: Array<LabelActor> = Array()

    private val tilesData: TileData = TileData
    private val unitsData: Units = Units

    private val tileSize = 16f //tile size used in the editor

    var actorsInitialized = false
        private set

    companion object {
        fun fromFile(fileName: String) : JsonLevel {
            val level =
                if (Gdx.files.internal("maps/$fileName.map").exists())
                    fromJsonString(Gdx.files.internal("maps/$fileName.map").readString())
                else
                    fromJsonString(Gdx.files.internal("maps/custom/$fileName.map").readString())

            level.fileName = fileName

            return level
        }

        fun fromJsonString(jsonString: String) : JsonLevel {
            val jsonObject = JsonReader().parse(jsonString)

            val level = JsonLevel()
            with(level) {
                name = jsonObject["name"].asString()

                jsonObject["game_mode"]?.let {
                    gameMode = it.asString()
                }

                jsonActors = jsonObject.get("actors")
            }

            return level
        }
    }

    fun initActors(assets: Assets) : JsonLevel {
        Gdx.app.log(JsonLevel::class.simpleName, "Reading $name... ")
        try {
            val jsonArray = jsonActors
            for (jsonActor in jsonArray) {
                val name = jsonActor["name"].asString()

                val positionX = jsonActor["x"].asFloat()
                val positionY = jsonActor["y"].asFloat()

                val layer = jsonActor["layer"].asInt()
                val rotation = jsonActor["transform"]?.get("rotation")?.asFloat()
                val flipX = jsonActor["transform"]?.get("flip_x")?.asBoolean()
                val flipY = jsonActor["transform"]?.get("flip_y")?.asBoolean()

                val jsonExtra = jsonActor["extra"]
                when (jsonExtra?.get("type")?.asString()) {
                    "unit" -> {
                        Gdx.app.error(JsonLevel::class.java.simpleName, "Skipping unit cos playerId is nullValue bug //fixme 523") //fixme 523

                        continue

                        val playerId = jsonExtra["player_id"]?.asInt() ?: Player.NONE
                        val teamId = jsonExtra["team_id"]?.asInt() ?: Team.NONE
                        val leaderId = jsonExtra["leader_id"]?.asInt() ?: -1
                        val followerId = jsonExtra["follower_id"]?.asInt() ?: -1
                        val allies = jsonExtra["allies"]?.asString()

                        val unit = unitsData.get(name)
                        unit.teamId = teamId
                        unit.playerId = playerId
                        if (leaderId > 0) {
                            unit.add(LeaderComponent(leaderId))
                        } else if (followerId > 0) {
                            unit.add(FollowerComponent(followerId))
                        }

                        if (allies != null) {
                            for (allyTeamId in allies.split(", ")) {
                                unit.allies.add(allyTeamId.toInt())
                                println("ally added: $allyTeamId")
                            }
                        }

                        unit.initView(assets)
                        unit.setPosition((positionX / tileSize).toInt(), (positionY / tileSize).toInt())

                        if (unitParsed(jsonActor, unit))
                            units.add(unit)
                    }

                    "tile" -> {
                        val tileData = tilesData.get(name) ?: throw Exception("Tile not found $name")

                        val tile = TileActor(tileData.name, tileData.terrain)

                        tile.initView()
                        tile.sprite.rotation = rotation ?: 0f
                        tile.sprite.setFlip(flipX ?: false, flipY ?: false)
                        tile.layer = layer

                        tile.setPosition((positionX / tileSize).toInt(), (positionY / tileSize).toInt())

                        // todo: fix this
                        if (tile.tileName == "blue_base")   setTileCrystalPlayer(tile, 1)
                        else if (tile.tileName == "red_base")   setTileCrystalPlayer(tile, 2)

                        jsonExtra["player_id"]?.let {
                            setTileCrystalPlayer(tile, it.asInt())
                        }

                        if (tileParsed(jsonActor, tile))
                            tiles.add(tile)
                    }

                    "label" -> {
                        val labelData = ArrayMap<String, String>()
                        for (extra in jsonExtra) {
                            if (extra.name == "type") continue

                            labelData.put(extra.name, extra.asString())
                        }

                        val label = LabelActor(name, labelData)
                        label.setPosition(
                            (positionX / tileSize).toInt().unTiled(),
                            (positionY / tileSize).toInt().unTiled()
                        )

                        if (labelParsed(jsonActor, label))
                            labels.add(label)
                    }
                }
            }

            actorsInitialized = true

            Gdx.app.log(JsonLevel::class.simpleName, "Done.")
        } catch (e: Exception) {
            Gdx.app.error(JsonLevel::class.simpleName, "Fail.", e)
        }

        return this
    }

    val JsonValue.extra get() = get("extra") ?: throw IllegalArgumentException("Json has no 'extra' value")

    /** if returns true, unit will be added to actors array */
    open fun unitParsed(json: JsonValue, unit: UnitActor) : Boolean {
        return true
    }

    open fun tileParsed(json: JsonValue, tile: TileActor) : Boolean {
        return true
    }

    open fun labelParsed(json: JsonValue, label: LabelActor) : Boolean {
        return true
    }

    override fun toString(): String {
        return if (name == "Level Name" && fileName.isNotEmpty()) fileName else name
    }
}
