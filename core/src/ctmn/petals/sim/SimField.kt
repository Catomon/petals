package ctmn.petals.sim

import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.map.label.LabelActor
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cUnit
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/**
 * Headless [BattleField] used by the dedicated server and by headless tests.
 *
 * It stores units, tiles and labels in plain lists and keeps small lookup maps so the
 * shared battle rules can query positions as fast as the scene2d backed `PlayStage`.
 */
class SimField : BattleField {

    private val actorsList = ArrayList<Actor>()
    private val unitsList = ArrayList<UnitActor>()
    private val tilesList = ArrayList<TileActor>()
    private val layer1List = ArrayList<TileActor>()
    private val labelsList = ArrayList<LabelActor>()

    private val unitGrid = HashMap<Long, UnitActor>()
    private val tileGrid = HashMap<Long, TileActor>()
    private val actorsByName = HashMap<String, Actor>()

    override var idCounter = 0

    override var tiledWidth = 0

    override var tiledHeight = 0

    private val newId get() = "%06d".format(idCounter++)

    override val allActors: List<Actor> get() = actorsList

    override val allUnits: List<UnitActor> get() = unitsList

    override val allTiles: List<TileActor> get() = tilesList

    override val layer1Tiles: List<TileActor> get() = layer1List

    override val labelActors: List<LabelActor> get() = labelsList

    private fun unitKey(x: Int, y: Int): Long = (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)

    private fun tileKey(x: Int, y: Int, layer: Int): Long =
        ((x.toLong() * 100_000L) + y) * 100_000L + (layer + 100_000L)

    override fun getUnit(x: Int, y: Int): UnitActor? = unitGrid[unitKey(x, y)]

    override fun getTile(x: Int, y: Int): TileActor? = tileGrid[tileKey(x, y, 1)]

    override fun getTile(x: Int, y: Int, layer: Int): TileActor? = tileGrid[tileKey(x, y, layer)]

    override fun findUnit(name: String): UnitActor? = actorsByName[name] as? UnitActor

    override fun tilesOfLayer(layer: Int): List<TileActor> = tilesList.filter { it.layer == layer }

    override fun addGameActor(actor: Actor) {
        actorsList.add(actor)
        (actor as? ctmn.petals.playstage.GameActor)?.field = this

        when (actor) {
            is TileActor -> {
                if (actor.name == null) actor.name = actor.tileName + "@" + newId

                if (tiledWidth < actor.tiledX + 1) tiledWidth = actor.tiledX + 1
                if (tiledHeight < actor.tiledY + 1) tiledHeight = actor.tiledY + 1

                tilesList.add(actor)
                if (actor.layer == 1) layer1List.add(actor)
                tileGrid[tileKey(actor.tiledX, actor.tiledY, actor.layer)] = actor
                actorsByName[actor.name!!] = actor
            }

            is UnitActor -> {
                if (actor.name == null) actor.name = actor.cUnit.name + "@" + newId

                if (tiledWidth < actor.tiledX + 1) tiledWidth = actor.tiledX + 1
                if (tiledHeight < actor.tiledY + 1) tiledHeight = actor.tiledY + 1

                unitsList.add(actor)
                unitGrid[unitKey(actor.tiledX, actor.tiledY)] = actor
                actorsByName[actor.name!!] = actor
            }

            is LabelActor -> labelsList.add(actor)
        }
    }

    override fun removeGameActor(actor: Actor) {
        actorsList.remove(actor)
        (actor as? ctmn.petals.playstage.GameActor)?.let { if (it.field === this) it.field = null }

        when (actor) {
            is TileActor -> {
                tilesList.remove(actor)
                layer1List.remove(actor)
                tileGrid.remove(tileKey(actor.tiledX, actor.tiledY, actor.layer))
                if (actor.name != null) actorsByName.remove(actor.name)
            }

            is UnitActor -> {
                unitsList.remove(actor)
                unitGrid.remove(unitKey(actor.tiledX, actor.tiledY))
                if (actor.name != null) actorsByName.remove(actor.name)
            }

            is LabelActor -> labelsList.remove(actor)
        }
    }

    override fun onUnitMoved(unit: UnitActor, oldX: Int, oldY: Int) {
        if (unitGrid[unitKey(oldX, oldY)] === unit) unitGrid.remove(unitKey(oldX, oldY))
        unitGrid[unitKey(unit.tiledX, unit.tiledY)] = unit
    }

    override fun onTileChanged(tile: TileActor) {
        rebuildTileIndex(emptyList())
        if (tile.layer == 1) {
            if (!layer1List.contains(tile)) layer1List.add(tile)
        } else {
            layer1List.remove(tile)
        }
    }

    override fun shiftLayerAt(tileX: Int, tileY: Int, shiftAmount: Int) {
        val atPosition = tilesList.filter { it.tiledX == tileX && it.tiledY == tileY }
        if (atPosition.isEmpty()) return

        rebuildTileIndex(except = atPosition)
        for (tile in atPosition) {
            tile.layer += shiftAmount
            if (tile.layer == 1 && !layer1List.contains(tile)) layer1List.add(tile)
            else if (tile.layer != 1) layer1List.remove(tile)
            tileGrid[tileKey(tile.tiledX, tile.tiledY, tile.layer)] = tile
        }
    }

    private fun rebuildTileIndex(except: List<TileActor>) {
        tileGrid.clear()
        for (tile in tilesList) {
            if (except.contains(tile)) continue
            tileGrid[tileKey(tile.tiledX, tile.tiledY, tile.layer)] = tile
        }
    }
}
