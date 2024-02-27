package ctmn.petals.multiplayer.json.serverres

import ctmn.petals.editor.MapSave

class MapResponse(val mapId: String, val mapSave: MapSave) {
    val id = "map"
}