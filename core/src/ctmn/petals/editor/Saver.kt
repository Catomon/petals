package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.GdxRuntimeException
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson

const val EDITOR_VERSION_UNSPECIFIED = "unspecified"

data class MapSave(
    var name: String,
    var layers: List<LayerSave>,
    val version: String = EDITOR_VERSION
)

data class LayerSave(
    var id: Int,
    var actors: List<TileSave>,
)

data class TileSave(
    var id: String,
    var x: Int,
    var y: Int,
)

fun CanvasStage.toMapSave(mapName: String? = null): MapSave {
    val layers = mutableListOf<LayerSave>()

    getLayers().forEach { layer ->
        layers.add(layer.toLayerSave())
    }

    return MapSave(mapName ?: "", layers, EDITOR_VERSION)
}

fun Group.toLayerSave(): LayerSave {
    val layerId = name?.toInt() ?: throw IllegalStateException("Group representing layer has no name")
    val tiles = mutableListOf<TileSave>()

    tiles.addAll(children.map {
        if (it is CanvasActor) {
            it.toTileSave()
        } else throw IllegalStateException("actor in layer Group is not CanvasActor")
    })

    return LayerSave(layerId, tiles)
}

fun CanvasActor.toTileSave(): TileSave {
    return TileSave(name, (x / tileSize).toInt(), (y / tileSize).toInt())
}

val MapSave.isOutdatedVersion get() = version == EDITOR_VERSION_UNSPECIFIED || version == null

class Saver(
    var fileName: String,
    var fileExtension: String = MAP_FILE_EXTENSION,
    var folder: String = MAPS_FOLDER_PATH,
) {

    val fileNameWithExtension get() = "$fileName.$fileExtension"
    private val fileHandle get() = Gdx.files.local("$folder/$fileNameWithExtension")

    fun exists() = fileHandle.exists()

    @Throws(GdxRuntimeException::class)
    fun saveMap(mapSave: MapSave) {
        if (mapSave.name.isEmpty()) mapSave.name = fileName
        val mapSaveJson = mapSave.toGson()
        fileHandle.writeString(mapSaveJson, false)
    }

    @Throws(GdxRuntimeException::class)
    fun loadMap(): MapSave {
        return fromGson(fileHandle.readString(), MapSave::class.java)
    }

    @Throws(GdxRuntimeException::class)
    fun deleteMap(): Boolean = fileHandle.delete()
}