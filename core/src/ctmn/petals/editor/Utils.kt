package ctmn.petals.editor


var tileSize = 100f

fun Float.toTilePos(): Int {
    return (this / tileSize).toInt()
}

fun Int.toEditorPos(): Float {
    return this * tileSize
}