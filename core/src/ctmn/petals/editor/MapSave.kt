package ctmn.petals.editor

import com.badlogic.gdx.scenes.scene2d.Group
import java.util.*
import kotlin.collections.HashMap

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