package ctmn.petals.map

import ctmn.petals.gameactors.label.LabelActor
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor
import com.badlogic.gdx.utils.Array

@Deprecated("silly")
interface Level {
    val tiles: Array<TileActor>
    val units: Array<UnitActor>
    val labels: Array<LabelActor>
}
