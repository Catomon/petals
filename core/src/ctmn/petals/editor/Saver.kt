package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson

data class MapSave(
    var name: String,
    var actors: List<TileSave>
)

data class TileSave(
    var id: String,
    var layer: Int,
    var x: Int,
    var y: Int
)

fun CanvasStage.asMapSave(mapName: String? = null): MapSave {
    return MapSave(
        mapName ?: "",
        getCanvasActors().map { it.asTileSave() }
    )
}

fun CanvasActor.asTileSave(): TileSave {
    return TileSave(name, layer, (x / tileSize).toInt(), (y / tileSize).toInt())
}

class Saver(
    var fileName: String,
) {

    var fileExtension = MAP_FILE_EXTENSION
    val folder = MAPS_FOLDER_PATH

    val fileNameWithExtension get() = "$fileName.$fileExtension"

    val fileHandle get() = Gdx.files.local("$folder/$fileNameWithExtension")

    fun exists() = fileHandle.exists()

    fun saveMap(mapSave: MapSave) {
        if (mapSave.name.isEmpty()) mapSave.name = fileName

        val mapSaveJson = mapSave.toGson()
        val mapFile: FileHandle = fileHandle
        mapFile.writeString(mapSaveJson, false)
    }

    fun loadMap(): MapSave {
        val mapFile: FileHandle = fileHandle
        return fromGson(mapFile.readString(), MapSave::class.java)
    }

    fun deleteMap(): Boolean {
        return fileHandle.delete()
    }
}