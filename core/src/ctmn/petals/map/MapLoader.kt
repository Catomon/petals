package ctmn.petals.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.GdxRuntimeException
import ctmn.petals.Const
import ctmn.petals.editor.CanvasActorsPackage
import ctmn.petals.editor.MAP_FILE_EXTENSION
import ctmn.petals.editor.MapSave
import ctmn.petals.editor.collectMaps
import ctmn.petals.map.label.LabelActor
import ctmn.petals.tile.*
import ctmn.petals.unit.*
import ctmn.petals.utils.logErr
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson
import java.io.FileNotFoundException

const val EXTRA_CREDITS_PER_BASE = "credits_per_base"
const val EXTRA_CREDITS_PER_CLUSTER = "credits_per_cluster"

val MapConverted.tiles get() = actors.filterIsInstance<TileActor>()
val MapConverted.units get() = actors.filterIsInstance<UnitActor>()
val MapConverted.labels get() = actors.filterIsInstance<LabelActor>()

fun MapSave.convertActors(): ArrayList<Actor> {
    val convertedActors = ArrayList<Actor>()

    for (layer in layers) {
        when {
            layer.id <= 2 ->
                actorsLoop@ for (actor in layer.actors) {
                    val tileData = TileData.get(actor.id)
                    if (tileData == null) {
                        Gdx.app.error("MapSave.convertActors()", "Tile ${actor.id} not found if its not a tile idc")
                        continue
                    }

                    convertedActors.add(
                        TileActor(tileData.name, tileData.terrain, layer.id, actor.x, actor.y).also { tile ->
                            tile.initView()
                        }
                    )
                }

            layer.id == CanvasActorsPackage.MARKERS_LAYER ->
                for (actor in layer.actors) {
                    convertedActors.add(LabelActor(actor.id, actor.x, actor.y))
                }

            layer.id == CanvasActorsPackage.UNITS_LAYER ->
                for (actor in layer.actors) {
                    val unit = Units.find(actor.id)
                        ?.also { it.position(actor.x, actor.y).player(playerIdByUnitSpecies(it)) }
                    if (unit == null) {
                        logErr("Unit not found: ${actor.id}")
                        continue
                    }

                    convertedActors.add(unit)
                }
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
        Gdx.files.internal("maps/$fileName.ptmap"),
        Gdx.files.internal("maps/default/$fileName.ptmap"),

        Gdx.files.local("maps/$fileName.ptmap"),
        Gdx.files.local("maps/custom/$fileName.ptmap"),
        Gdx.files.local("maps/default/$fileName.ptmap"),
        Gdx.files.local("maps/shared/$fileName.ptmap"),
    )

    val existingPath =
        paths.firstOrNull { it.exists() } ?: throw FileNotFoundException("Level file with name $fileName not found")

    return createMapFromJson(existingPath.readString())
}

fun loadScenarioMap(fileName: String): MapConverted {
    if (!Const.IS_RELEASE) {
        val custom = Gdx.files.internal("maps/custom/$fileName.ptmap")
        if (custom.exists())
            return createMapFromJson(custom.readString())
    }
    return createMapFromJson(Gdx.files.internal("maps/scenario/$fileName.ptmap").readString())
}

fun loadMapById(mapId: String): MapConverted? {
    for (mapItem in collectMaps()) {
        val mapSave = mapItem.mapSave
        if (mapId == mapSave.id)
            return MapConverted(mapSave)
    }

    return null
}