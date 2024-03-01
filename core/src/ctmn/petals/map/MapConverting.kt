package ctmn.petals.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.GdxRuntimeException
import ctmn.petals.editor.MAP_FILE_EXTENSION
import ctmn.petals.editor.MapSave
import ctmn.petals.map.label.LabelActor
import ctmn.petals.tile.*
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson
import java.io.FileNotFoundException

const val EXTRA_CREDITS_PER_BASE = "credits_per_base"
const val EXTRA_CREDITS_PER_CLUSTER = "credits_per_cluster"

class MapConverted(
    val mapSave: MapSave,
) {
    val mapId get() = mapSave.id ?: ""
    val gameMode get() = mapSave.extra?.get("game_mode") as String? ?: ""

    val actors by lazy { convertActors() }

    var maxPlayers: Int = 0
    val playerBases = ArrayList<TileActor>(8)

    private fun convertActors(): ArrayList<Actor> {
        val converted = mapSave.convertActors()

        val bases = converted.filterIsInstance<TileActor>().filter { it.isPlaceholderBaseTile() }
            .onEach { setPlayerForCapturableTile(it, placeholderBaseNameToPlayerId(it.tileName)) }
            .sortedBy { it.cPlayerId!!.playerId }
        var labelId = 0
        bases.groupBy { it.cPlayerId!!.playerId }.forEach { (_, group) ->
            group.forEach { base ->
                converted.add(LabelActor("player", base.tiledX, base.tiledY).apply {
                    data.put("id", labelId.toString())
                })
            }
            labelId++
        }

        playerBases.addAll(bases)

        // 1 primary base per player
//        primaryBases.addAll(converted.filterIsInstance<TileActor>().filter { it.isUnassignedPrimaryBaseTile() }
//            .groupBy { it.tileComponent.name }.entries.map { it.value.first() })
//
//        // remove excess primary bases, so they don't trash the map
//        converted.removeAll { it.isUnassignedPrimaryBaseTile() && !primaryBases.contains(it) }
//
//        for ((labels, primaryBase) in primaryBases.withIndex()) {
//            converted.add(LabelActor("player", primaryBase.tiledX, primaryBase.tiledY).apply {
//                data.put("id", labels.toString())
//            })
//        }

        maxPlayers = playerBases.map { it.tileComponent.name }.toSet().size

        return converted
    }
}

val MapConverted.tiles get() = actors.filterIsInstance<TileActor>()
val MapConverted.units get() = actors.filterIsInstance<UnitActor>()
val MapConverted.labels get() = actors.filterIsInstance<LabelActor>()

fun MapSave.convertActors(): ArrayList<Actor> {
    val convertedActors = ArrayList<Actor>()

    for (layer in layers) {
        actorsLoop@ for (actor in layer.actors) {
            val tileData = TileData.get(actor.id)
            if (tileData == null) {
                Gdx.app.error("MapSave.convertActors()", "Tile ${actor.id} not found 💀 if its not a tile idc")
                continue
            }

            convertedActors.add(
                TileActor(tileData.name, tileData.terrain, layer.id, actor.x, actor.y).also { tile ->
                    tile.initView()
                }
            )
        }
    }

    return convertedActors
}

@Throws(GdxRuntimeException::class)
fun saveSharedMap(mapSave: MapSave): String {
    //val fileName = "${mapSave.name}_${Random.nextInt(1000, 999999999)}"
    val fileName = mapSave.name

    var fileHandle = Gdx.files.local("maps/shared/$fileName.$MAP_FILE_EXTENSION")

    var num = 1
    while (fileHandle.exists()) {
        num++
        fileHandle = Gdx.files.local("maps/shared/${fileName}_$num.$MAP_FILE_EXTENSION")
    }

    if (mapSave.name.isEmpty()) mapSave.name = fileName
    val mapSaveJson = mapSave.toGson()
    fileHandle.writeString(mapSaveJson, false)

    return fileName
}

fun createMapFromJson(json: String) = MapConverted(fromGson(json, MapSave::class.java))

fun loadMap(fileName: String): MapConverted {
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

    val existingPath =
        paths.firstOrNull { it.exists() } ?: throw FileNotFoundException("Level file with name $fileName not found")

    return createMapFromJson(existingPath.readString())
}

fun loadMapById(mapId: String): MapConverted? {
    val defMaps = Gdx.files.internal("maps")
    val customMaps = Gdx.files.internal("maps/custom")
    val sharedMaps = Gdx.files.internal("maps/shared")
    for (path in defMaps.list() + customMaps.list() + sharedMaps.list()) {
        if (path.isDirectory) continue

        val mapSave = createMapFromJson(path.readString())
        if (mapId == mapSave.mapId)
            return mapSave
    }

    return null
}