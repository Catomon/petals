package ctmn.petals.editor

// size of a CanvasActor
var tileSize = 100f

fun Float.toTilePos(): Int {
    return (this / tileSize).toInt()
}

fun Int.toEditorPos(): Float {
    return this * tileSize
}