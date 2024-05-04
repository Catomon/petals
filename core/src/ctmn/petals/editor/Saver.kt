package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson

class MapItem(
    val fileHandle: FileHandle,
    val type: Type,
) {
    val mapSave by lazy { fromGson(fileHandle.readString(), MapSave::class.java) }

    enum class Type {
        DEFAULT, CUSTOM, SHARED, UNLISTED
    }
}

fun collectMaps(): ArrayList<MapItem> {
    val defMaps = Gdx.files.internal("maps/default")
    val customMaps = Gdx.files.local("maps/custom")
    val sharedMaps = Gdx.files.local("maps/shared")
    val unlistedMaps = Gdx.files.internal("maps/")

    val maps = ArrayList<MapItem>()
    for (path in defMaps.list()) {
        if (path.isDirectory) continue
        maps.add(MapItem(path, MapItem.Type.DEFAULT))
    }
    for (path in customMaps.list()) {
        if (path.isDirectory) continue
        maps.add(MapItem(path, MapItem.Type.CUSTOM))
    }
    for (path in sharedMaps.list()) {
        if (path.isDirectory) continue
        maps.add(MapItem(path, MapItem.Type.SHARED))
    }
    for(path in unlistedMaps.list()) {
        if (path.isDirectory) continue
        maps.add(MapItem(path, MapItem.Type.UNLISTED))
    }

    return maps
}

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
    fun saveMap(mapSave: MapSave, override: Boolean = false) {
        if (fileName.isEmpty()) {
            if (mapSave.name.isNotEmpty())
                fileName = mapSave.name
            else
                throw IllegalStateException("File/Map name should not be empty")
        } else {
            if (mapSave.name.isEmpty()) mapSave.name = fileName
        }

        if (!override) {
            var num = 1
            while (exists()) {
                num++
                if (fileName[fileName.length - 1].isDigit() && fileName[fileName.length - 2] == '_')
                    fileName = fileName.substring(0, fileName.length - 2)

                fileName += "_$num"
            }
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