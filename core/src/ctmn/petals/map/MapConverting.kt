package ctmn.petals.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.GdxRuntimeException
import ctmn.petals.editor.MAP_FILE_EXTENSION
import ctmn.petals.editor.MapSave
import ctmn.petals.gameactors.label.LabelActor
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.tile.setTileCrystalPlayer
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson
import java.io.FileNotFoundException
import kotlin.random.Random

const val EXTRA_CREDITS_PER_BASE = "credits_per_base"
const val EXTRA_CREDITS_PER_CLUSTER = "credits_per_cluster"

class MapConverted(
    val mapSave: MapSave,
) {
    val mapId get() = mapSave.id ?: ""
    val gameMode get() = mapSave.extra?.get("game_mode") as String? ?: ""

    val actors by lazy {
        mapSave.convertActors()
    }
}

val MapConverted.tiles get() = actors.filterIsInstance<TileActor>()
val MapConverted.units get() = actors.filterIsInstance<UnitActor>()
val MapConverted.labels get() = actors.filterIsInstance<LabelActor>()

fun MapSave.convertActors(): Array<Actor> {
    val convertedActors = ArrayList<Actor>()
    for (layer in layers) {
        actorsLoop@ for (actor in layer.actors) {
            val tileData = TileData.get(actor.id)
            if (tileData == null) {
                Gdx.app.error("MapSave.convert()", "Tile not found 💀 if its not a tile idc")
                continue
            }

            convertedActors.add(
                TileActor(tileData.name, tileData.terrain, layer.id, actor.x, actor.y).also { tile ->
                    tile.initView()
                    if (tile.tileName == "blue_base") setTileCrystalPlayer(tile, 1)
                    else if (tile.tileName == "red_base") setTileCrystalPlayer(tile, 2)
                    //TODO
                }
            )
        }
    }

    return convertedActors.toTypedArray()
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

    val existingPath = paths.firstOrNull { it.exists() } ?: throw FileNotFoundException("Level file with name $fileName not found")

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