package ctmn.petals.level

import ctmn.petals.gameactors.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor
import com.badlogic.gdx.utils.Array

interface Level {
    val players: Array<Player>
    val tiles: Array<TileActor>
    val units: Array<UnitActor>
    val labels: Array<LabelActor>
}
