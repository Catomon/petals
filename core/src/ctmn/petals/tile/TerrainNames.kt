package ctmn.petals.tile

object TerrainNames {
    const val base = "base"
    const val castle = "castle"
    const val impassable = "impassable"
    const val grass = "grass"
    const val forest = "forest"
    const val mountains = "mountains"
    const val hills = "hills"
    const val water = "water"
    const val roads = "roads"
    const val walls = "walls"
    const val highwall = "highwall"
    const val indoors = "indoors"
    const val unwalkable = "unwalkable"
    const val fog = "fog" // fog tiles are place at 10 layer
    const val earthcrack = "earthcrack"
    const val tower = "tower"
    const val fortress = "fortress"
    const val fallenforest = "fallenforest"
    const val swamp = "swamp"
    const val crystals = "crystals"
    const val deepwater = "deepwater"
    const val skyscraper = "skyscraper"
    const val chasm = "chasm"
    const val lava = "lava"

    // is not a real terrain, terrain prop pack might have it as UNREACHABLE
    // to indicate that unit should not get to land capturables
    const val land_capturable = "land_capturable"
}
