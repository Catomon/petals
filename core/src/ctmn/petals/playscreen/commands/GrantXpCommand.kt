package ctmn.petals.playscreen.commands

import ctmn.petals.Config
import ctmn.petals.playscreen.events.UnitLevelUpEvent
import ctmn.petals.playstage.getLeadUnit
import ctmn.petals.sim.Battle
import ctmn.petals.unit.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.unit.xpToLevelUp
import com.badlogic.gdx.graphics.Color
import ctmn.petals.playscreen.stageName
import ctmn.petals.unit.UnitActor

class GrantXpCommand(val unitId: String, val amount: Int) : Command() {

    constructor(unit: UnitActor, amount: Int) : this(unit.stageName, amount)

    override fun canExecute(battle: Battle): Boolean {
        var unit: UnitActor = battle.field.findUnit(unitId) ?: return false

        //if the unit is Follower and his Leader is dead, he will not grant any xp
        if (unit.isFollower) {
            unit = battle.field.getLeadUnit(unit.followerID) ?: return false
        }

        val cLevel = unit.cLevel ?: return false

        if (cLevel.lvl == Config.MAX_LVL) return false

        return true
    }

    override fun execute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId) ?: return false
        val cLevel = unit.cLevel ?: return false

        cLevel.exp += amount

        battle.showFloatingLabel("+$amount XP", Color.BLUE, unit.centerX, unit.centerY)

        val oldLvl = cLevel.lvl

        //if enough exp to level up
        while (cLevel.exp >= xpToLevelUp(cLevel.lvl) && cLevel.lvl < Config.MAX_LVL) {
            cLevel.exp -= xpToLevelUp(cLevel.lvl)
            cLevel.lvl++

            if (cLevel.lvl >= Config.MAX_LVL)
                cLevel.exp = xpToLevelUp(Config.MAX_LVL - 1)

            battle.showFloatingLabel("LEVEL UP!", Color.YELLOW, unit.centerX, unit.centerY)
        }

        if(oldLvl != cLevel.lvl) {
            unit.levelUp()

            battle.fireEvent(UnitLevelUpEvent(unit))
        }

        return true
    }
}
