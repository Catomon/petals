package ctmn.petals.level

import ctmn.petals.Assets
import ctmn.petals.gameactors.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.unit.*
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ArrayMap
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import ctmn.petals.tile.setTileCrystalPlayer
import ctmn.petals.unit.UnitActor
import java.io.FileNotFoundException

/** Usage: JsonLevel.fromFile(name).initActors(assets) */
open class JsonLevel private constructor() : Level {

    var petalsEditor = false

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
        fun fromFile(fileName: String): JsonLevel {
            val paths = listOf(
                Gdx.files.internal("maps/$fileName.map"),
                Gdx.files.internal("maps/custom/$fileName.map"),
                Gdx.files.internal("maps/$fileName.ptmap"),
                Gdx.files.internal("maps/custom/$fileName.ptmap"),

                Gdx.files.local("maps/$fileName.map"),
                Gdx.files.local("maps/custom/$fileName.map"),
                Gdx.files.local("maps/$fileName.ptmap"),
                Gdx.files.local("maps/custom/$fileName.ptmap"),
            )

            val existingPath = paths.firstOrNull { it.exists() } ?: throw FileNotFoundException("Level file with name $fileName not found")

            val level = fromJsonString(existingPath.readString())

            return level.apply {
                this.fileName = fileName
                this.petalsEditor = existingPath.path().contains(".ptmap")
            }
        }

        fun fromJsonString(jsonString: String): JsonLevel {
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

    fun initActors(assets: Assets): JsonLevel {
        Gdx.app.log(JsonLevel::class.simpleName, "Reading $name... ")
        try {
            val jsonArray = jsonActors
            for (jsonActor in jsonArray) {

                val name =
                    if (jsonActor.has("name"))
                        jsonActor["name"].asString()
                    else
                        jsonActor["id"].asString()

                val positionX = if (petalsEditor) jsonActor["x"].asFloat() * tileSize else jsonActor["x"].asFloat()
                val positionY = if (petalsEditor) jsonActor["y"].asFloat() * tileSize else jsonActor["y"].asFloat()


                val layer = if (jsonActor.has("layer")) jsonActor["layer"].asInt() else let {
                    Gdx.app.error(this::class.simpleName, "jsonActor has no layer value, will set 1")
                    1
                }
                val rotation = jsonActor["transform"]?.get("rotation")?.asFloat()
                val flipX = jsonActor["transform"]?.get("flip_x")?.asBoolean()
                val flipY = jsonActor["transform"]?.get("flip_y")?.asBoolean()

                val jsonExtra = if (jsonActor.has("extra")) jsonActor["extra"] else null
                when (jsonExtra?.get("type")?.asString() ?: let {
                    Gdx.app.error(
                        this::class.simpleName,
                        "jsonActor has no type value or has no extra value, guess its tile"
                    )
                    "tile"
                }) {
                    "unit" -> {
                        if (jsonExtra == null) continue

                        Gdx.app.error(
                            JsonLevel::class.java.simpleName,
                            "Skipping unit cos playerId is nullValue bug //fixme 523"
                        ) //fixme 523
//                        val playerId = jsonExtra["player_id"]?.asInt() ?: Player.NONE
//                        val teamId = jsonExtra["team_id"]?.asInt() ?: Team.NONE
//                        val leaderId = jsonExtra["leader_id"]?.asInt() ?: -1
//                        val followerId = jsonExtra["follower_id"]?.asInt() ?: -1
//                        val allies = jsonExtra["allies"]?.asString()
//
//                        val unit = unitsData.get(name)
//                        unit.teamId = teamId
//                        unit.playerId = playerId
//                        if (leaderId > 0) {
//                            unit.add(LeaderComponent(leaderId))
//                        } else if (followerId > 0) {
//                            unit.add(FollowerComponent(followerId))
//                        }
//
//                        if (allies != null) {
//                            for (allyTeamId in allies.split(", ")) {
//                                unit.allies.add(allyTeamId.toInt())
//                                println("ally added: $allyTeamId")
//                            }
//                        }
//
//                        unit.initView(assets)
//                        unit.setPosition((positionX / tileSize).toInt(), (positionY / tileSize).toInt())
//
//                        if (unitParsed(jsonActor, unit))
//                            units.add(unit)
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
                        if (tile.tileName == "blue_base") setTileCrystalPlayer(tile, 1)
                        else if (tile.tileName == "red_base") setTileCrystalPlayer(tile, 2)

                        jsonExtra?.get("player_id")?.let {
                            setTileCrystalPlayer(tile, it.asInt())
                        }

                        if (tileParsed(jsonActor, tile))
                            tiles.add(tile)
                    }

                    "label" -> {
                        if (jsonExtra == null) continue

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
    open fun unitParsed(json: JsonValue, unit: UnitActor): Boolean {
        return true
    }

    open fun tileParsed(json: JsonValue, tile: TileActor): Boolean {
        return true
    }

    open fun labelParsed(json: JsonValue, label: LabelActor): Boolean {
        return true
    }

    override fun toString(): String {
        return if (name == "Level Name" && fileName.isNotEmpty()) fileName else name
    }
}
