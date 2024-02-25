package ctmn.petals.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.editor.MapSave
import ctmn.petals.gameactors.label.LabelActor
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.tile.setTileCrystalPlayer
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.fromGson
import java.io.FileNotFoundException

class MapConverted(
    val fileName: String,
    val mapSave: MapSave,
) {
    var gameMode = "not_implemented"

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

fun loadMapFromJson(fileName: String, json: String): MapConverted {
    val mapSave = fromGson(json, MapSave::class.java)

    return MapConverted(
        fileName,
        mapSave
    )
}

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

    return loadMapFromJson(fileName, existingPath.readString())
}