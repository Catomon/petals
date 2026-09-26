package ctmn.petals.sim

import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.map.label.LabelActor
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor

/**
 * The render free view of a battle field.
 *
 * Implemented by the scene2d backed `PlayStage` (local and online client play) and by
 * [SimField] (headless dedicated server). All battle rules query the field through this
 * interface, so the exact same command and turn logic runs with or without libGDX rendering.
 *
 * When a [UnitActor] or [TileActor] lives in a [SimField] it has no scene2d stage, so the
 * existing `playStageOrNull` view guards automatically turn every visual effect into a no-op.
 */
interface BattleField {

    var idCounter: Int

    var tiledWidth: Int

    var tiledHeight: Int

    /** Every actor in the field: units, tiles and labels. */
    val allActors: List<Actor>

    val allUnits: List<UnitActor>

    /** Tiles of every layer. */
    val allTiles: List<TileActor>

    /** Tiles of the main (layer 1) grid. */
    val layer1Tiles: List<TileActor>

    val labelActors: List<LabelActor>

    fun getUnit(x: Int, y: Int): UnitActor?

    /** Tile of the main layer at [x], [y]. */
    fun getTile(x: Int, y: Int): TileActor?

    fun getTile(x: Int, y: Int, layer: Int): TileActor?

    fun findUnit(name: String): UnitActor?

    fun tilesOfLayer(layer: Int): List<TileActor>

    /** Adds a unit, tile or label to the field. */
    fun addGameActor(actor: Actor)

    /** Removes a unit, tile or label from the field. */
    fun removeGameActor(actor: Actor)

    /** Moves every tile at [tileX], [tileY] up or down [shiftAmount] layers. */
    fun shiftLayerAt(tileX: Int, tileY: Int, shiftAmount: Int)

    /** Called after a unit changed its tiled position, so positional caches can update. */
    fun onUnitMoved(unit: UnitActor, oldX: Int, oldY: Int) {}

    /** Called after a tile changed its layer or name, so positional caches can update. */
    fun onTileChanged(tile: TileActor) {}
}
