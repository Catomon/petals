package ctmn.petals.playscreen.commands

import ctmn.petals.Const.ACTION_POINTS_ATTACK
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.selfName
import ctmn.petals.playscreen.stageName
import ctmn.petals.playstage.destroyTile
import ctmn.petals.playstage.removeTileSafely
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.DestroyingComponent
import ctmn.petals.tile.isCapturable
import ctmn.petals.unit.*
import ctmn.petals.unit.component.InvisibilityComponent

class DestroyTileCommand(val unitId: String, val baseId: String) : Command() {

    constructor(unit: UnitActor, base: TileActor) : this(unit.stageName, base.stageName)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)
        val tile: TileActor = playScreen.playStage.root.findActor(baseId)

        if (unit.actionPoints <= 0) return false

        if (!tile.isCapturable)
            throw IllegalArgumentException("The tile terrain is not capturable")

        if (!unit.canDestroy(tile, playScreen.turnManager.getPlayerById(tile.cPlayerId?.playerId ?: -1)?.teamId))
            return false

        return true
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)
        val tile: TileActor = playScreen.playStage.root.findActor(baseId)

        unit.actionPoints -= ACTION_POINTS_ATTACK

        unit.del(InvisibilityComponent::class.java)

        val playStage = playScreen.playStage
        if (unit.selfName == UnitIds.HUNTER || unit.selfName == UnitIds.GOBLIN_GIANT) {
            playStage.destroyTile(tile)
            //playStage.removeTileSafely(tile)

        } else {
            tile.add(DestroyingComponent(unit.playerId))
        }

        return true
    }
}