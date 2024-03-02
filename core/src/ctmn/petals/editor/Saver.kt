package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.GdxRuntimeException
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson
import java.util.UUID

data class MapSave(
    var name: String,
    var layers: List<LayerSave>,
    val extra: HashMap<String, Any> = hashMapOf(),
    val version: String = EDITOR_VERSION,
    var id: String = UUID.randomUUID().toString(),
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

    return MapSave(
        mapName ?: "",
        layers,
        hashMapOf(),
        EDITOR_VERSION,
        UUID.randomUUID().toString()
    )
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

val MapSave.isOutdatedVersion get() = version == null || version < MAP_MIN_VERSION

class Saver(
    fileName: String = "",
    var fileExtension: String = MAP_FILE_EXTENSION,
    var folder: String = MAPS_FOLDER_PATH,
) {

    var fileName: String = fileName
        set(name) {
            field = makeValidFileName(name)
        }

    private val fileNameWithExtension get() = "$fileName.$fileExtension"
    private val fileHandle get() = Gdx.files.local("$folder/$fileNameWithExtension")

    fun exists() = fileHandle.exists()

    @Throws(GdxRuntimeException::class)
    fun saveMap(mapSave: MapSave) {
        if (fileName.isEmpty()) {
            if (mapSave.name.isNotEmpty())
                fileName = mapSave.name
            else
                throw IllegalStateException("File/Map name should not be empty")
        } else {
            if (mapSave.name.isEmpty()) mapSave.name = fileName
        }

        var num = 1
        while (exists()) {
            num++
            if (fileName[fileName.length - 1].isDigit() && fileName[fileName.length - 2] == '_')
                fileName = fileName.substring(0, fileName.length - 2)

            fileName += "_$num"
        }

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