package ctmn.petals.playscreen.commands

import ctmn.petals.GameConst
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.effects.FloatingLabelAnimation
import ctmn.petals.playscreen.events.UnitLevelUpEvent
import ctmn.petals.playstage.getLeadUnit
import ctmn.petals.unit.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.xpToLevelUp
import com.badlogic.gdx.graphics.Color
import ctmn.petals.playscreen.stageName
import ctmn.petals.unit.UnitActor

class GrantXpCommand(val unitId: String, val amount: Int) : Command() {

    constructor(unit: UnitActor, amount: Int) : this(unit.stageName, amount)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        var unit: UnitActor = playScreen.playStage.root.findActor(unitId) ?: return false

        //if the unit is Follower and his Leader is dead, he will not grant any xp
        if (unit.isFollower) {
            unit = playScreen.playStage.getLeadUnit(unit.followerID) ?: return false
        }

        val cLevel = unit.cLevel ?: return false

        if (cLevel.lvl == GameConst.MAX_LVL) return false

        return true
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId) ?: return false
        val cLevel = unit.cLevel ?: return false

        cLevel.exp += amount

        val label = FloatingLabelAnimation("+$amount XP", "font_5")
        label.color = Color.BLUE
        label.setPosition(unit.centerX - label.width / 4, unit.centerY)
        playScreen.playStage.addActor(label)

        val oldLvl = cLevel.lvl

        //if enough exp to level up
        while (cLevel.exp >= xpToLevelUp(cLevel.lvl) && cLevel.lvl < GameConst.MAX_LVL) {
            cLevel.exp -= xpToLevelUp(cLevel.lvl)
            cLevel.lvl++

            if (cLevel.lvl >= GameConst.MAX_LVL)
                cLevel.exp = xpToLevelUp(GameConst.MAX_LVL - 1)

            val label = FloatingLabelAnimation("LEVEL UP!", "font_5")
            label.color = Color.YELLOW
            label.setPosition(unit.centerX - label.width / 4, unit.centerY)
            playScreen.playStage.addActor(label)
        }

        if(oldLvl != cLevel.lvl) {
            unit.levelUp()

            playScreen.fireEvent(UnitLevelUpEvent(unit))
        }

        return true
    }
}