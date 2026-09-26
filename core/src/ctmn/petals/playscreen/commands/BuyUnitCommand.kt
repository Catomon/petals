package ctmn.petals.playscreen.commands

import ctmn.petals.playscreen.events.UnitBoughtEvent
import ctmn.petals.sim.Battle
import ctmn.petals.unit.*
import ctmn.petals.player.Tech
import ctmn.petals.player.Techs
import ctmn.petals.playscreen.selfName
import ctmn.petals.tile.components.ActionCooldown
import ctmn.petals.tile.isBase
import ctmn.petals.utils.GameLog

class BuyUnitCommand(val unitName: String, val buyerPlayerId: Int, var cost: Int = -1, val tileX: Int, val tileY: Int, val leaderId: Int = -1) : Command() {

    override fun canExecute(battle: Battle): Boolean {
        //command executes only on a castle terrain
        if (false) //tile?.tile?.terrain == "castle"
            return false

        //check if a castle terrain tile is occupied by other unit
        if (battle.field.getUnit(tileX, tileY) != null)
            return false

        //find player
        val player = battle.turnManager.getPlayerById(buyerPlayerId) ?: return false

        //check if player has not enough gold
        if (player.credits < cost)
            return false

        //get unit by name
        val unitActor = battle.unitsData.get(unitName, player)

        //cost
        if (cost < 0)
            cost = unitActor.cShop?.price ?: return false

        return true
    }

    override fun execute(battle: Battle): Boolean {
        val player = battle.turnManager.getPlayerById(buyerPlayerId) ?: return false
        val unitActor = battle.unitsData.get(unitName, player)

        player.credits -= cost

        if (unitActor.isFollower && leaderId != -1)
            unitActor.followerID = leaderId

        unitActor.actionPoints = 0
        unitActor.setPosition(tileX, tileY)
        battle.field.addGameActor(unitActor)

        val tile = battle.field.getTile(tileX, tileY)
        if (tile?.isBase == true) {
            tile.add(ActionCooldown())
        }

        for (techName in player.techs) {
            val tech = Techs.map[techName]
            if (tech?.targetType == Tech.TargetType.BoughtUnit) {
                tech.applyTechToUnit(unitActor)
            }
        }

        //sent event to the PlayStage
        battle.fireEvent(UnitBoughtEvent(unitActor))

        //log
        GameLog.verbose("BuyUnitCommand", "Unit $unitName was bought at $tileX:$tileY")

        return true
    }
}
