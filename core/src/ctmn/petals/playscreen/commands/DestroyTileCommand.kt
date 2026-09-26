package ctmn.petals.playscreen.commands

import ctmn.petals.Config.ACTION_POINTS_ATTACK
import ctmn.petals.playscreen.selfName
import ctmn.petals.playscreen.stageName
import ctmn.petals.playstage.destroyTile
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.DestroyingComponent
import ctmn.petals.tile.isBase
import ctmn.petals.unit.*
import ctmn.petals.unit.component.InvisibilityComponent

class DestroyTileCommand(val unitId: String, val baseId: String) : Command() {

    constructor(unit: UnitActor, base: TileActor) : this(unit.stageName, base.stageName)

    override fun canExecute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId)!!
        val tile: TileActor = battle.field.allTiles.firstOrNull { it.name == baseId }!!

        if (unit.actionPoints <= 0) return false

        if (!(tile.isBase || tile.terrain == TerrainNames.building))
            throw IllegalArgumentException("The tile terrain is not destroyable")

        if (!unit.canDestroy(tile, battle.turnManager.getPlayerById(tile.cPlayerId?.playerId ?: -1)?.teamId))
            return false

        return true
    }

    override fun execute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId)!!
        val tile: TileActor = battle.field.allTiles.firstOrNull { it.name == baseId }!!

        unit.actionPoints -= ACTION_POINTS_ATTACK

        unit.del(InvisibilityComponent::class.java)

        if (unit.selfName == UnitIds.HUNTER || unit.selfName == UnitIds.GOBLIN_GIANT) {
            battle.field.destroyTile(tile)
            //battle.field.removeTileSafely(tile)

        } else {
            tile.add(DestroyingComponent(unit.playerId))
        }

        return true
    }
}
