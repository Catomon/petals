package ctmn.petals.map

import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.editor.MapSave
import ctmn.petals.map.label.LabelActor
import ctmn.petals.tile.*

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