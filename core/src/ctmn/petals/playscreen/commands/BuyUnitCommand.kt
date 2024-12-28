package ctmn.petals.playscreen.commands

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.UnitBoughtEvent
import ctmn.petals.unit.*
import com.badlogic.gdx.Gdx
import ctmn.petals.player.Tech
import ctmn.petals.player.Techs
import ctmn.petals.playscreen.selfName
import ctmn.petals.tile.components.ActionCooldown
import ctmn.petals.tile.isBase

class BuyUnitCommand(val unitName: String, val buyerPlayerId: Int, var cost: Int = -1, val tileX: Int, val tileY: Int, val leaderId: Int = -1) : Command() {

    override fun canExecute(playScreen: PlayScreen): Boolean {
        //command executes only on a castle terrain
        if (false) //tile?.tile?.terrain == "castle"
            return false

        //check if a castle terrain tile is occupied by other unit
        if (playScreen.playStage.getUnit(tileX, tileY) != null)
            return false

        //find player
        val player = playScreen.turnManager.getPlayerById(buyerPlayerId) ?: return false

        //check if player has not enough gold
        if (player.credits < cost)
            return false

        //get unit by name
        val unitActor = playScreen.unitsData.get(unitName, player)

        //cost
        if (cost < 0)
            cost = unitActor.cShop?.price ?: return false

        return true
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val player = playScreen.turnManager.getPlayerById(buyerPlayerId) ?: return false
        val unitActor = playScreen.unitsData.get(unitName, player)

        player.credits -= cost

        if (unitActor.isFollower && leaderId != -1)
            unitActor.followerID = leaderId

        unitActor.actionPoints = 0
        unitActor.setPosition(tileX, tileY)
        playScreen.playStage.addActor(unitActor)

        val tile = playScreen.playStage.getTile(tileX, tileY)
        if (tile?.isBase == true) {
            tile.add(ActionCooldown())
        }

        for (techName in player.techs) {
            val tech = Techs.map[techName]
            if (tech?.targetType == Tech.TargetType.BoughtUnit && (tech.targetId.isEmpty() || tech.targetId == unitActor.selfName)) {
                tech.applyTech(unitActor)
            }
        }

        //sent event to the PlayStage
        playScreen.fireEvent(UnitBoughtEvent(unitActor))

        //log
        Gdx.app.log(BuyUnitCommand::class.simpleName, "Unit $unitName was bought at $tileX:$tileY")

        return true
    }
}
